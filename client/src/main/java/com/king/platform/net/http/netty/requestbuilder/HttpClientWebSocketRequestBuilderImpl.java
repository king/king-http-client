// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.HttpClientCaller;
import com.king.platform.net.http.netty.sse.VoidResponseConsumer;
import com.king.platform.net.http.netty.websocket.WebSocketClientImpl;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.Executor;


public class HttpClientWebSocketRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientWebSocketRequestBuilder> implements HttpClientWebSocketRequestBuilder {
	public HttpClientWebSocketRequestBuilderImpl(HttpClientCaller httpClientCaller, String uri, ConfMap confMap,
												 Executor callbackExecutor) {
		super(HttpClientWebSocketRequestBuilder.class, httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.GET, uri, confMap, callbackExecutor);
	}

	@Override
	public BuiltWebSocketRequest build() {
		final BuiltNettyClientRequest<Void> builtNettyClientRequest = new BuiltNettyClientRequest<>(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, acceptCompressedResponse, keepAlive, null, null, null, queryParameters,
			headerParameters, callbackExecutor, VoidResponseConsumer::new);

		return new BuiltWebSocketRequest() {
			@Override
			public WebSocketClient execute(WebSocketClientCallback webSocketClientCallback) {
				WebSocketClientImpl webSocketClient = new WebSocketClientImpl(webSocketClientCallback, builtNettyClientRequest, callbackExecutor);
				webSocketClient.connect();
				return webSocketClient;
			}
		};

	}


}
