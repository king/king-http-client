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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


public class HttpClientWebSocketRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientWebSocketRequestBuilder> implements HttpClientWebSocketRequestBuilder {
	private final Executor defaultCallbackExecutor;
	private boolean autoPong;
	private Duration pingEveryDuration;
	private boolean autoCloseFrame;

	public HttpClientWebSocketRequestBuilderImpl(HttpClientCaller httpClientCaller, String uri, ConfMap confMap,
												 Executor callbackExecutor) {
		super(HttpClientWebSocketRequestBuilder.class, httpClientCaller, HttpVersion.HTTP_1_1, HttpMethod.GET, uri, confMap, callbackExecutor);
		this.defaultCallbackExecutor = callbackExecutor;
		autoPong(confMap.get(ConfKeys.WEB_SOCKET_AUTO_PONG));
		autoCloseFrame(confMap.get(ConfKeys.WEB_SOCKET_AUTO_CLOSE_FRAME));
	}

	@Override
	public HttpClientWebSocketRequestBuilder subProtocols(String subProtocols) {
		addHeader(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, subProtocols);
		return this;
	}

	@Override
	public HttpClientWebSocketRequestBuilder pingEvery(Duration duration) {
		this.pingEveryDuration = duration;
		return this;
	}

	@Override
	public HttpClientWebSocketRequestBuilder autoPong(boolean autoPong) {
		this.autoPong = autoPong;
		return this;
	}

	@Override
	public HttpClientWebSocketRequestBuilder autoCloseFrame(boolean autoCloseFrame) {
		this.autoCloseFrame = autoCloseFrame;
		return this;
	}

	@Override
	public BuiltWebSocketRequest build() {

		totalRequestTimeoutMillis(0); //disable total timeouts

		final BuiltNettyClientRequest<Void> builtNettyClientRequest = new BuiltNettyClientRequest<>(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, acceptCompressedResponse, keepAlive, automaticallyDecompressResponse, null, null, null, queryParameters,
			headerParameters, callbackExecutor, VoidResponseConsumer::new);


		return new BuiltWebSocketRequest() {

			@Override
			public CompletableFuture<WebSocketClient> execute(WebSocketListener webSocketListener) {

				WebSocketClientImpl webSocketClient = create();

				webSocketClient.addListener(webSocketListener);

				return webSocketClient.connect();

			}

			@Override
			public WebSocketClient build() {
				return create();
			}

			private WebSocketClientImpl create() {
				Executor listenerExecutor = null;
				if (defaultCallbackExecutor != HttpClientWebSocketRequestBuilderImpl.this.callbackExecutor) {
					listenerExecutor = HttpClientWebSocketRequestBuilderImpl.this.callbackExecutor;
				} else {
					listenerExecutor = Runnable::run; //if no executor has been supplied (ie, still on default executor), run on calling thread
				}

				return new WebSocketClientImpl(builtNettyClientRequest, listenerExecutor, listenerExecutor, autoPong, autoCloseFrame, pingEveryDuration);
			}


		};

	}




}
