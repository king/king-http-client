// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.backpressure.BackPressure;
import com.king.platform.net.http.netty.eventbus.*;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import com.king.platform.net.http.netty.requestbuilder.HttpClientRequestBuilderImpl;
import com.king.platform.net.http.netty.requestbuilder.HttpClientRequestWithBodyBuilderImpl;
import com.king.platform.net.http.netty.requestbuilder.HttpClientSseRequestBuilderImpl;
import com.king.platform.net.http.netty.requestbuilder.HttpClientWebSocketRequestBuilderImpl;
import com.king.platform.net.http.netty.response.HttpClientResponseHandler;
import com.king.platform.net.http.netty.response.HttpRedirector;
import com.king.platform.net.http.netty.util.TimeProvider;
import com.king.platform.net.http.netty.websocket.WebSocketHandler;
import com.king.platform.net.http.netty.websocket.WebSocketResponseHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class NettyHttpClient implements HttpClient {
	private final AtomicBoolean started = new AtomicBoolean();

	private final ConfMap confMap = new ConfMap();
	private final Executor defaultHttpClientCallbackExecutor;
	private final Timer cleanupTimer;
	private final TimeProvider timeProvider;

	private final Logger logger = getLogger(getClass());

	private final int nioThreads;
	private final ThreadFactory nioThreadFactory;
	private final RootEventBus rootEventBus;
	private final ChannelPool channelPool;

	private EventLoopGroup group;
	private BackPressure executionBackPressure;

	private List<ShutdownJob> shutdownJobs = new ArrayList<>();
	private HttpClientCallerImpl httpClientCaller;

	public NettyHttpClient(int nioThreads, ThreadFactory nioThreadFactory, Executor defaultHttpClientCallbackExecutor, Timer
		cleanupTimer, TimeProvider timeProvider, final BackPressure executionBackPressure, RootEventBus rootEventBus, ChannelPool channelPool) {
		this.defaultHttpClientCallbackExecutor = defaultHttpClientCallbackExecutor;

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
		if (!started.compareAndSet(false, true)) {
			throw new IllegalStateException("Http client has already been started!");
		}

		if (Epoll.isAvailable() && confMap.get(ConfKeys.USE_EPOLL)) {
			group = new EpollEventLoopGroup(nioThreads, nioThreadFactory);
		} else {
			group = new NioEventLoopGroup(nioThreads, nioThreadFactory);
		}

		HttpClientResponseHandler responseHandler = new HttpClientResponseHandler(new HttpRedirector());
		HttpClientRequestHandler requestHandler = new HttpClientRequestHandler();
		HttpClientHandler clientHandler = new HttpClientHandler(responseHandler, requestHandler);
		WebSocketResponseHandler webSocketResponseHandler = new WebSocketResponseHandler();
		WebSocketHandler webSocketHandler = new WebSocketHandler(webSocketResponseHandler,  requestHandler);

		ChannelManager channelManager = new ChannelManager(group, clientHandler, webSocketHandler, cleanupTimer, timeProvider, channelPool, confMap, rootEventBus);

		boolean executeOnCallingThread = confMap.get(ConfKeys.EXECUTE_ON_CALLING_THREAD);

		httpClientCaller = new HttpClientCallerImpl(rootEventBus, executeOnCallingThread, channelManager, executionBackPressure, timeProvider);
	}

	@Override
	public void shutdown() {
		if (!started.compareAndSet(true, false)) {
			throw new IllegalStateException("Http client is not running!");
		}

		if (group != null) {
			group.shutdownGracefully(0, 10, TimeUnit.SECONDS);
		}

		for (ShutdownJob shutdownJob : shutdownJobs) {
			shutdownJob.onShutdown();
		}

	}

	@Override
	public HttpClientRequestBuilder createGet(String uri) {
		verifyStarted();
		return new HttpClientRequestBuilderImpl(httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.GET, uri, confMap, defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientRequestWithBodyBuilder createPost(String uri) {
		verifyStarted();
		return new HttpClientRequestWithBodyBuilderImpl(httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.POST, uri, confMap,
			defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientRequestWithBodyBuilder createPut(String uri) {
		verifyStarted();
		return new HttpClientRequestWithBodyBuilderImpl(httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.PUT, uri, confMap,
			defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientRequestBuilder createDelete(String uri) {
		verifyStarted();
		return new HttpClientRequestBuilderImpl(httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.DELETE, uri, confMap, defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientRequestBuilder createHead(String uri) {
		verifyStarted();
		return new HttpClientRequestBuilderImpl(httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.HEAD, uri, confMap, defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientSseRequestBuilder createSSE(String uri) {
		verifyStarted();
		return new HttpClientSseRequestBuilderImpl(httpClientCaller, uri, confMap, defaultHttpClientCallbackExecutor);
	}

	@Override
	public HttpClientWebSocketRequestBuilder createWebSocket(String uri) {
		verifyStarted();
		return new HttpClientWebSocketRequestBuilderImpl(httpClientCaller, uri, confMap, defaultHttpClientCallbackExecutor);
	}

	private void verifyStarted() {
		if (!started.get()) {
			throw new IllegalStateException("Http client is not running!");
		}
	}

	<T> void setOption(ConfKeys<T> key, T value) {
		if (started.get()) {
			throw new IllegalStateException("Can't set global config keys after the client has been started!");
		}

		confMap.set(key, value);
	}

	public void addShutdownJob(ShutdownJob shutdownJob) {
		shutdownJobs.add(shutdownJob);
	}


	interface ShutdownJob {
		void onShutdown();
	}

}
