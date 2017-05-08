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

	public TestingHttpClientFactory() {
		recordingEventBus = new RecordingEventBus(new DefaultEventBus());
	}

	public HttpClient create() {

		HttpClient httpClient = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2)
			.setRootEventBus(recordingEventBus)
			.setChannelPool(new NoChannelPool())
			.createHttpClient();


		httpClient.setConf(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES, true);
		httpClient.setConf(ConfKeys.HTTP_FOLLOW_REDIRECTS, true);
		httpClient.setConf(ConfKeys.NETTY_TRACE_LOGS, true);

		httpClient.setConf(ConfKeys.ACCEPT_COMPRESSED_RESPONSE, false);


		return httpClient;

	}

	public RecordingEventBus getRecordingEventBus() {
		return recordingEventBus;
	}
}
