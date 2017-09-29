// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;

import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.backpressure.BackPressure;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.RootEventBus;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.util.Timer;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class TestingHttpClientFactory {


	private RecordingEventBus recordingEventBus;
	private NettyHttpClientBuilder nettyHttpClientBuilder;

	public TestingHttpClientFactory() {
		recordingEventBus = new RecordingEventBus(new DefaultEventBus());
		nettyHttpClientBuilder = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(1)
			.setRootEventBus(recordingEventBus)
			.setChannelPool(new NoChannelPool())
			.setOption(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES, true)
			.setOption(ConfKeys.HTTP_FOLLOW_REDIRECTS, true)
			.setOption(ConfKeys.NETTY_TRACE_LOGS, true)
			.setOption(ConfKeys.ACCEPT_COMPRESSED_RESPONSE, false);
	}

	public TestingHttpClientFactory setNioThreads(int nioThreads) {
		nettyHttpClientBuilder.setNioThreads(nioThreads);
		return this;
	}

	public TestingHttpClientFactory setMetricCallback(MetricCallback metricCallback) {
		nettyHttpClientBuilder.setMetricCallback(metricCallback);
		return this;
	}

	public TestingHttpClientFactory setHttpCallbackExecutorThreads(int httpCallbackExecutorThreads) {
		nettyHttpClientBuilder.setHttpCallbackExecutorThreads(httpCallbackExecutorThreads);
		return this;
	}

	public TestingHttpClientFactory setHttpCallbackExecutor(Executor executor) {
		nettyHttpClientBuilder.setHttpCallbackExecutor(executor);
		return this;
	}

	public TestingHttpClientFactory setNioThreadFactory(ThreadFactory nioThreadFactory) {
		nettyHttpClientBuilder.setNioThreadFactory(nioThreadFactory);
		return this;
	}

	public TestingHttpClientFactory setCleanupTimer(Timer cleanupTimer) {
		nettyHttpClientBuilder.setCleanupTimer(cleanupTimer);
		return this;
	}

	public TestingHttpClientFactory setTimeProvider(TimeProvider timeProvider) {
		nettyHttpClientBuilder.setTimeProvider(timeProvider);
		return this;
	}

	public TestingHttpClientFactory setExecutionBackPressure(BackPressure executionBackPressure) {
		nettyHttpClientBuilder.setExecutionBackPressure(executionBackPressure);
		return this;
	}

	public TestingHttpClientFactory setRootEventBus(RootEventBus rootEventBus) {
		nettyHttpClientBuilder.setRootEventBus(rootEventBus);
		return this;
	}

	public TestingHttpClientFactory setChannelPool(ChannelPool channelPool) {
		nettyHttpClientBuilder.setChannelPool(channelPool);
		return this;
	}

	public TestingHttpClientFactory setKeepAliveTimeoutMs(int ms) {
		nettyHttpClientBuilder.setKeepAliveTimeoutMs(ms);
		return this;
	}

	public HttpClient create() {
		HttpClient httpClient = nettyHttpClientBuilder.createHttpClient();
		return httpClient;
	}

	public <T> TestingHttpClientFactory setOption(ConfKeys<T> key, T value) {
		nettyHttpClientBuilder.setOption(key, value);
		return this;
	}

	public RecordingEventBus getRecordingEventBus() {
		return recordingEventBus;
	}
}
