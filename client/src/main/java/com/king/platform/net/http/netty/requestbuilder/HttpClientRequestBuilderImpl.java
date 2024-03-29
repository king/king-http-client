// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltClientRequest;
import com.king.platform.net.http.HttpClientRequestBuilder;
import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.StringResponseBody;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.HttpClientCaller;
import com.king.platform.net.http.netty.ServerInfoValidator;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class HttpClientRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientRequestBuilder> implements HttpClientRequestBuilder {

	public HttpClientRequestBuilderImpl(HttpClientCaller httpClientCaller, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap, Executor
		callbackExecutor) {
		super(HttpClientRequestBuilder.class, httpClientCaller, httpVersion, httpMethod, uri, confMap, callbackExecutor);
	}

	@Override
	public BuiltClientRequest<String> build() {
		return build(StringResponseBody::new);
	}

	@Override
	public <T> BuiltClientRequest<T> build(Supplier<ResponseBodyConsumer<T>> responseBodyConsumer) {
		return new BuiltNettyClientRequest<T>(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent, idleTimeoutMillis, totalRequestTimeoutMillis,
			followRedirects, acceptCompressedResponse, keepAlive, keepAliveTimeoutMillis, automaticallyDecompressResponse, null, null, null, queryParameters, headerParameters, callbackExecutor, responseBodyConsumer, null, ServerInfoValidator.HTTP);
	}
}
