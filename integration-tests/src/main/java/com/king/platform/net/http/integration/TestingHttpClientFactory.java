// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;

import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.pool.NoChannelPool;

public class TestingHttpClientFactory {


	private RecordingEventBus recordingEventBus;
	private NettyHttpClientBuilder nettyHttpClientBuilder;

	public TestingHttpClientFactory() {
		recordingEventBus = new RecordingEventBus(new DefaultEventBus());
		nettyHttpClientBuilder = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2)
			.setRootEventBus(recordingEventBus)
			.setChannelPool(new NoChannelPool())
			.setOption(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES, true)
			.setOption(ConfKeys.HTTP_FOLLOW_REDIRECTS, true)
			.setOption(ConfKeys.NETTY_TRACE_LOGS, true)
			.setOption(ConfKeys.ACCEPT_COMPRESSED_RESPONSE, false);
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
