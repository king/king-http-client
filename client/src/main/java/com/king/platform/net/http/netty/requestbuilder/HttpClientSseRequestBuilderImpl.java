// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltSseClientRequest;
import com.king.platform.net.http.HttpClientSseRequestBuilder;
import com.king.platform.net.http.SseClient;
import com.king.platform.net.http.SseExecutionCallback;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.HttpClientCaller;
import com.king.platform.net.http.netty.sse.SseClientImpl;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.Executor;

public class HttpClientSseRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientSseRequestBuilder> implements HttpClientSseRequestBuilder {
	public HttpClientSseRequestBuilderImpl(HttpClientCaller httpClientCaller, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap,
										   Executor callbackExecutor) {
		super(HttpClientSseRequestBuilder.class, httpClientCaller, httpVersion, httpMethod, uri, confMap, callbackExecutor);
	}

	@Override
	public BuiltSseClientRequest build() {

		withHeader("Accept", "text/event-stream");

		final BuiltNettyClientRequest builtNettyClientRequest = new BuiltNettyClientRequest(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, acceptCompressedResponse, keepAlive, null, null, null, queryParameters,
			headerParameters, callbackExecutor);


		return new BuiltSseClientRequest() {
			@Override
			public SseClient execute(SseExecutionCallback providedSseExecutionCallback) {
				SseClientImpl sseClient = new SseClientImpl(providedSseExecutionCallback, builtNettyClientRequest, callbackExecutor);
				sseClient.connect();
				return sseClient;
			}

			@Override
			public SseClient execute() {
				SseClientImpl sseClient = new SseClientImpl(null, builtNettyClientRequest, callbackExecutor);
				sseClient.connect();
				return sseClient;
			}

			@Override
			public SseClient build(SseExecutionCallback sseExecutionCallback) {
				return new SseClientImpl(sseExecutionCallback, builtNettyClientRequest, callbackExecutor);
			}

			@Override
			public SseClient build() {
				return new SseClientImpl(null, builtNettyClientRequest, callbackExecutor);
			}
		};

	}


}
