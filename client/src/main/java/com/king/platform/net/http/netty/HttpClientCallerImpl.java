package com.king.platform.net.http.netty;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.backpressure.BackPressure;
import com.king.platform.net.http.netty.eventbus.*;
import com.king.platform.net.http.netty.metric.TimeStampRecorder;
import com.king.platform.net.http.netty.request.NettyHttpClientRequest;
import com.king.platform.net.http.netty.requestbuilder.UploadCallbackInvoker;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpClientCallerImpl implements HttpClientCaller {

	private final Logger logger = getLogger(getClass());
	private final RootEventBus rootEventBus;
	private final boolean executeOnCallingThread;
	private final BackPressure executionBackPressure;
	private final TimeProvider timeProvider;

	HttpClientCallerImpl(RootEventBus rootEventBus, boolean executeOnCallingThread, BackPressure executionBackPressure, TimeProvider timeProvider) {
		this.rootEventBus = rootEventBus;
		this.executeOnCallingThread = executeOnCallingThread;
		this.executionBackPressure = executionBackPressure;
		this.timeProvider = timeProvider;
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> execute(HttpMethod httpMethod, final NettyHttpClientRequest<T> nettyHttpClientRequest,
														  HttpCallback<T> httpCallback, final NioCallback nioCallback, UploadCallback uploadCallback,
														  ResponseBodyConsumer<T> responseBodyConsumer, Executor callbackExecutor,
														  ExternalEventTrigger externalEventTrigger, CustomCallbackSubscriber customCallbackSubscriber, int idleTimeoutMillis, int totalRequestTimeoutMillis,
														  boolean followRedirects, boolean keepAlive, int keepAliveTimeoutMillis, boolean automaticallyDecompressResponse,
														  WebSocketConf webSocketConf) {

		final RequestEventBus requestRequestEventBus = rootEventBus.createRequestEventBus();

		if (externalEventTrigger != null) {
			externalEventTrigger.registerEventListener(new EventListener() {
				@Override
				public <E> void onEvent(Event1<E> event, E payload) {
					requestRequestEventBus.triggerEvent(event, payload);
				}

				@Override
				public <E1, E2> void onEvent(Event2<E1, E2> event, E1 payload1, E2 payload2) {
					requestRequestEventBus.triggerEvent(event, payload1, payload2);
				}
			});
		}

		subscribeToHttpCallbackEvents(callbackExecutor, httpCallback, requestRequestEventBus);
		subscribeToNioCallbackEvents(nioCallback, requestRequestEventBus);
		subscribeToUploadCallbacksEvents(callbackExecutor, uploadCallback, requestRequestEventBus);

		if (customCallbackSubscriber != null) {
			customCallbackSubscriber.subscribeOn(requestRequestEventBus);
		}

		if (responseBodyConsumer == null) {

			responseBodyConsumer = getResponseBodyConsumer();
		}


		final HttpRequestContext<T> httpRequestContext = new HttpRequestContext<>(httpMethod, nettyHttpClientRequest, requestRequestEventBus,
			responseBodyConsumer,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, keepAlive, keepAliveTimeoutMillis, new TimeStampRecorder(timeProvider), automaticallyDecompressResponse,
			webSocketConf);

		ResponseFuture<T> future = new ResponseFuture<>(requestRequestEventBus, httpRequestContext, callbackExecutor);

		if (!executionBackPressure.acquireSlot(nettyHttpClientRequest.getServerInfo())) {
			requestRequestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new KingHttpException("Too many concurrent connections"));
			return future;
		}

		logger.trace("Executing httpRequest {}", httpRequestContext);

		if (executeOnCallingThread) {
			sendRequest(requestRequestEventBus, httpRequestContext);
		} else {
			callbackExecutor.execute(() -> sendRequest(requestRequestEventBus, httpRequestContext));
		}

		return future;
	}


	@SuppressWarnings("unchecked")
	private <T> ResponseBodyConsumer<T> getResponseBodyConsumer() {
		return (ResponseBodyConsumer<T>) EMPTY_RESPONSE_BODY_CONSUMER;
	}

	private <T> void subscribeToHttpCallbackEvents(final Executor callbackExecutor, final HttpCallback<T> httpCallback, RequestEventBus
		requestRequestEventBus) {
		if (httpCallback == null) {
			return;
		}
		HttpCallbackInvoker<T> httpCallbackInvoker = new HttpCallbackInvoker<>(callbackExecutor, httpCallback);
		requestRequestEventBus.subscribePermanently(Event.COMPLETED, httpCallbackInvoker::onCompleted);
		requestRequestEventBus.subscribePermanently(Event.ERROR, httpCallbackInvoker::onError);

	}

	private void subscribeToNioCallbackEvents(final NioCallback nioCallback, RequestEventBus requestRequestEventBus) {
		if (nioCallback == null) {
			return;
		}

		requestRequestEventBus.subscribePermanently(Event.onConnecting, (payload) -> nioCallback.onConnecting());
		requestRequestEventBus.subscribePermanently(Event.onConnected, (payload) -> nioCallback.onConnected());
		requestRequestEventBus.subscribePermanently(Event.onWroteHeaders, (payload) -> nioCallback.onWroteHeaders());
		requestRequestEventBus.subscribePermanently(Event.onWroteContentProgressed, nioCallback::onWroteContentProgressed);
		requestRequestEventBus.subscribePermanently(Event.onWroteContentCompleted, (payload) -> nioCallback.onWroteContentCompleted());
		requestRequestEventBus.subscribePermanently(Event.onReceivedStatus, nioCallback::onReceivedStatus);
		requestRequestEventBus.subscribePermanently(Event.onReceivedHeaders, nioCallback::onReceivedHeaders);
		requestRequestEventBus.subscribePermanently(Event.onReceivedContentPart, nioCallback::onReceivedContentPart);
		requestRequestEventBus.subscribePermanently(Event.onReceivedCompleted, nioCallback::onReceivedCompleted);
		requestRequestEventBus.subscribePermanently(Event.ERROR, (httpRequestContext, throwable) -> nioCallback.onError(throwable));
	}

	private void subscribeToUploadCallbacksEvents(Executor callbackExecutor, UploadCallback uploadCallback, RequestEventBus requestRequestEventBus) {
		if (uploadCallback == null) {
			return;
		}

		UploadCallbackInvoker uploadCallbackInvoker = new UploadCallbackInvoker(uploadCallback, callbackExecutor);
		requestRequestEventBus.subscribe(Event.onWroteContentStarted, uploadCallbackInvoker::onUploadStarted);
		requestRequestEventBus.subscribe(Event.onWroteContentProgressed, uploadCallbackInvoker::onUploadProgressed);
		requestRequestEventBus.subscribe(Event.onWroteContentCompleted, uploadCallbackInvoker::onUploadComplete);

	}

	private <T> void sendRequest(RequestEventBus requestRequestEventBus, HttpRequestContext<T> httpRequestContext) {
		requestRequestEventBus.triggerEvent(Event.EXECUTE_REQUEST, httpRequestContext);

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

}
