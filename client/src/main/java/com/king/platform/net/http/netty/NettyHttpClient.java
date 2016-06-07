// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.backpressure.BackPressure;
import com.king.platform.net.http.netty.eventbus.*;
import com.king.platform.net.http.netty.metric.TimeStampRecorder;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import com.king.platform.net.http.netty.request.NettyHttpClientRequest;
import com.king.platform.net.http.netty.requestbuilder.HttpClientRequestBuilderImpl;
import com.king.platform.net.http.netty.requestbuilder.HttpClientRequestWithBodyBuilderImpl;
import com.king.platform.net.http.netty.response.HttpClientResponseHandler;
import com.king.platform.net.http.netty.response.HttpRedirector;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class NettyHttpClient implements HttpClient {
	private final AtomicBoolean started = new AtomicBoolean();

	private final ConfMap confMap = new ConfMap();
	private final Executor httpClientCallbackExecutor;
	private final Executor httpClientExecuteExecutor;
	private final Timer cleanupTimer;
	private final TimeProvider timeProvider;

	private final Logger logger = getLogger(getClass());

	private final int nioThreads;
	private final ThreadFactory nioThreadFactory;
	private final RootEventBus rootEventBus;
	private final ChannelPool channelPool;

	private NioEventLoopGroup group;
	private ChannelManager channelManager;
	private BackPressure executionBackPressure;
	private Boolean executeOnCallingThread;

	private List<ShutdownJob> shutdownJobs = new ArrayList<>();

	public NettyHttpClient(int nioThreads, ThreadFactory nioThreadFactory, Executor httpClientCallbackExecutor, Executor httpClientExecuteExecutor, Timer
		cleanupTimer, TimeProvider timeProvider, final BackPressure executionBackPressure, RootEventBus rootEventBus, ChannelPool channelPool) {
		this.httpClientCallbackExecutor = httpClientCallbackExecutor;
		this.httpClientExecuteExecutor = httpClientExecuteExecutor;
		this.cleanupTimer = cleanupTimer;
		this.timeProvider = timeProvider;
		this.nioThreads = nioThreads;
		this.nioThreadFactory = nioThreadFactory;
		this.executionBackPressure = executionBackPressure;
		this.rootEventBus = rootEventBus;
		this.channelPool = channelPool;


		rootEventBus.subscribePermanently(Event.COMPLETED, new EventBusCallback1<HttpRequestContext>() {
			@Override
			public void onEvent(Event1<HttpRequestContext> event, HttpRequestContext httpRequestContext) {
				executionBackPressure.releaseSlot(httpRequestContext.getServerInfo());
			}
		});

		rootEventBus.subscribePermanently(Event.ERROR, new EventBusCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onEvent(Event2<HttpRequestContext, Throwable> event, HttpRequestContext httpRequestContext, Throwable throwable) {
				executionBackPressure.releaseSlot(httpRequestContext.getServerInfo());
			}
		});

	}

	@Override
	public void start() {
		if (started.get()) {
			throw new IllegalStateException("Http client already started!");
		}

		started.set(true);

		executeOnCallingThread = confMap.get(ConfKeys.EXECUTE_ON_CALLING_THREAD);

		group = new NioEventLoopGroup(nioThreads, nioThreadFactory);

		HttpClientResponseHandler responseHandler = new HttpClientResponseHandler(new HttpRedirector());
		HttpClientRequestHandler requestHandler = new HttpClientRequestHandler();
		HttpClientHandler clientHandler = new HttpClientHandler(responseHandler, requestHandler);

		channelManager = new ChannelManager(group, clientHandler, cleanupTimer, timeProvider, channelPool, confMap, rootEventBus);
	}


	@Override
	public void shutdown() {
		started.set(false);

		if (group != null) {
			group.shutdownGracefully(0, 10, TimeUnit.SECONDS);
		}

		for (ShutdownJob shutdownJob : shutdownJobs) {
			shutdownJob.onShutdown();
		}

	}

	@Override
	public <T> void setConf(ConfKeys<T> key, T value) {
		if (started.get()) {
			throw new RuntimeException("Can't set global config keys after the client has been started");
		}

		confMap.set(key, value);
	}


	public <T> Future<FutureResult<T>> execute(final NettyHttpClientRequest<T> nettyHttpClientRequest, HttpCallback<T> httpCallback, final
	NioCallback nioCallback, ResponseBodyConsumer<T> responseBodyConsumer, int idleTimeoutMillis, int totalRequestTimeoutMillis, boolean followRedirects,
	                                              boolean keepAlive) {

		if (!started.get()) {
			throw new IllegalStateException("Http client is not started!");
		}

		httpCallback = runOnlyOnceWrapper(httpCallback);

		final RequestEventBus requestRequestEventBus = rootEventBus.createRequestEventBus();

		subscribeToHttpCallbackEvents(httpCallback, requestRequestEventBus);
		subscribeToNioCallbackEvents(nioCallback, requestRequestEventBus);


		if (responseBodyConsumer == null) {
			responseBodyConsumer = (ResponseBodyConsumer<T>) EMPTY_RESPONSE_BODY_CONSUMER;
		}


		final HttpRequestContext<T> httpRequestContext = new HttpRequestContext<>(nettyHttpClientRequest, requestRequestEventBus, responseBodyConsumer,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, keepAlive, new TimeStampRecorder(timeProvider));

		ResponseFuture<T> future = new ResponseFuture<>(requestRequestEventBus, httpRequestContext);

		if (!executionBackPressure.acquireSlot(nettyHttpClientRequest.getServerInfo())) {
			requestRequestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new KingHttpException("Too many concurrent connections"));
			return future;
		}

		logger.trace("Executing httpRequest {}", httpRequestContext);

		if (executeOnCallingThread) {
			sendRequest(requestRequestEventBus, httpRequestContext);
		} else {
			httpClientExecuteExecutor.execute(new Runnable() {
				@Override
				public void run() {
					sendRequest(requestRequestEventBus, httpRequestContext);
				}
			});
		}

		return future;
	}

	private <T> HttpCallback<T> runOnlyOnceWrapper(final HttpCallback<T> httpCallback) {
		if (httpCallback == null) {
			return null;
		}

		return new HttpCallback<T>() {
			private final AtomicBoolean firstExecute = new AtomicBoolean();
			@Override
			public void onCompleted(HttpResponse<T> httpResponse) {
				if (firstExecute.compareAndSet(false, true)) {
					httpCallback.onCompleted(httpResponse);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				if (firstExecute.compareAndSet(false, true)) {
					httpCallback.onError(throwable);
				}
			}
		};
	}

	private <T> void sendRequest(RequestEventBus requestRequestEventBus, HttpRequestContext<T> httpRequestContext) {
		try {
			channelManager.sendOnChannel(httpRequestContext, requestRequestEventBus);
		} catch (Throwable throwable) {
			requestRequestEventBus.triggerEvent(Event.ERROR, httpRequestContext, throwable);
		}
	}


	@Override
	public HttpClientRequestBuilder createGet(String uri) {
		if (!started.get()) {
			throw new IllegalStateException("Http client is not started!");
		}

		return new HttpClientRequestBuilderImpl(this, HttpVersion.HTTP_1_1, HttpMethod.GET, uri, confMap);
	}

	@Override
	public HttpClientRequestWithBodyBuilder createPost(String uri) {
		if (!started.get()) {
			throw new IllegalStateException("Http client is not started!");
		}

		return new HttpClientRequestWithBodyBuilderImpl(this, HttpVersion.HTTP_1_1, HttpMethod.POST, uri, confMap);
	}

	@Override
	public HttpClientRequestWithBodyBuilder createPut(String uri) {
		if (!started.get()) {
			throw new IllegalStateException("Http client is not started!");
		}

		return new HttpClientRequestWithBodyBuilderImpl(this, HttpVersion.HTTP_1_1, HttpMethod.PUT, uri, confMap);
	}

	@Override
	public HttpClientRequestBuilder createDelete(String uri) {
		if (!started.get()) {
			throw new IllegalStateException("Http client is not started!");
		}

		return new HttpClientRequestBuilderImpl(this, HttpVersion.HTTP_1_1, HttpMethod.DELETE, uri, confMap);
	}


	private <T> void subscribeToHttpCallbackEvents(final HttpCallback<T> httpCallback, RequestEventBus requestRequestEventBus) {
		if (httpCallback == null) {
			return;
		}

		requestRequestEventBus.subscribePermanently(Event.onHttpResponseDone, new RunOnceCallback1<HttpResponse>() {
			@Override
			public void onFirstEvent(Event1 event, final HttpResponse httpResponse) {
				httpClientCallbackExecutor.execute(new Runnable() {
					@Override
					public void run() {
						httpCallback.onCompleted(httpResponse);
					}
				});
			}
		});

		requestRequestEventBus.subscribePermanently(Event.ERROR, new RunOnceCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onFirstEvent(Event2 event, HttpRequestContext httpRequestContext, final Throwable throwable) {
				httpClientCallbackExecutor.execute(new Runnable() {
					@Override
					public void run() {
						httpCallback.onError(throwable);
					}
				});
			}
		});


	}

	public <T> Future<FutureResult<T>> dispatchError(final HttpCallback<T> httpCallback, final Throwable throwable) {
		if (httpCallback != null) {
			httpClientCallbackExecutor.execute(new Runnable() {
				@Override
				public void run() {
					httpCallback.onError(throwable);
				}
			});
		}

		return ResponseFuture.error(throwable);

	}

	public void addShutdownJob(ShutdownJob shutdownJob) {
		shutdownJobs.add(shutdownJob);
	}

	private void subscribeToNioCallbackEvents(final NioCallback nioCallback, RequestEventBus requestRequestEventBus) {
		if (nioCallback == null) {
			return;
		}

		requestRequestEventBus.subscribePermanently(Event.onConnecting, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Event1 event, Void payload) {
				nioCallback.onConnecting();
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onConnected, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Event1 event, Void payload) {
				nioCallback.onConnected();
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onWroteHeaders, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Event1 event, Void payload) {
				nioCallback.onWroteHeaders();
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onWroteContentProgressed, new EventBusCallback2<Long, Long>() {
			@Override
			public void onEvent(Event2<Long, Long> event, Long progress, Long total) {
				nioCallback.onWroteContentProgressed(progress, total);
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onWroteContentCompleted, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Event1 event, Void payload) {
				nioCallback.onWroteContentCompleted();
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onReceivedStatus, new EventBusCallback1<HttpResponseStatus>() {
			@Override
			public void onEvent(Event1<HttpResponseStatus> event, HttpResponseStatus payload) {
				nioCallback.onReceivedStatus(payload);
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onReceivedHeaders, new EventBusCallback1<HttpHeaders>() {
			@Override
			public void onEvent(Event1<HttpHeaders> event, HttpHeaders payload) {
				nioCallback.onReceivedHeaders(payload);
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onReceivedContentPart, new EventBusCallback2<Integer, ByteBuf>() {
			@Override
			public void onEvent(Event2<Integer, ByteBuf> event, Integer length, ByteBuf contentPart) {
				nioCallback.onReceivedContentPart(length, contentPart);
			}
		});

		requestRequestEventBus.subscribePermanently(Event.onReceivedCompleted, new EventBusCallback2<HttpResponseStatus, HttpHeaders>() {
			@Override
			public void onEvent(Event2<HttpResponseStatus, HttpHeaders> event, HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {
				nioCallback.onReceivedCompleted(httpResponseStatus, httpHeaders);
			}
		});

		requestRequestEventBus.subscribePermanently(Event.ERROR, new EventBusCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onEvent(Event2<HttpRequestContext, Throwable> event, HttpRequestContext httpRequestContext, Throwable throwable) {
				nioCallback.onError(throwable);
			}
		});
	}

	private static final ResponseBodyConsumer<Void> EMPTY_RESPONSE_BODY_CONSUMER = new ResponseBodyConsumer<Void>() {
		@Override
		public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
		}

		@Override
		public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		}

		@Override
		public void onCompletedBody() throws Exception {
		}

		@Override
		public Void getBody() {
			return null;
		}
	};


	interface ShutdownJob {
		void onShutdown();
	}

}
