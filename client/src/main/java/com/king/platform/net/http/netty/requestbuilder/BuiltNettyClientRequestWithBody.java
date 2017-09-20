// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.HttpClientCaller;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.util.Param;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BuiltNettyClientRequestWithBody extends BuiltNettyClientRequest implements BuiltClientRequestWithBody {
	public BuiltNettyClientRequestWithBody(HttpClientCaller httpClientCaller, HttpVersion httpVersion, HttpMethod httpMethod, String uri, String
		defaultUserAgent, int idleTimeoutMillis, int totalRequestTimeoutMillis, boolean followRedirects, boolean acceptCompressedResponse, boolean keepAlive,
										   RequestBodyBuilder requestBodyBuilder, String contentType, Charset bodyCharset, List<Param> queryParameters,
										   List<Param> headerParameters, Executor callbackExecutor) {
		super(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent, idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects,
			acceptCompressedResponse, keepAlive, requestBodyBuilder, contentType, bodyCharset, queryParameters, headerParameters, callbackExecutor);
	}

	@Override
	public CompletableFuture<HttpResponse<String>> execute(UploadCallback uploadCallback) {
		return internalExecute(new StringResponseBody(), null, null, null, uploadCallback);

	}

	@Override
	public CompletableFuture<HttpResponse<String>> execute(HttpCallback<String> httpCallback, UploadCallback uploadCallback) {
		return internalExecute(new StringResponseBody(), null, null, httpCallback, uploadCallback);
	}

	@Override
	public CompletableFuture<HttpResponse<String>> execute(HttpCallback<String> httpCallback, NioCallback nioCallback, UploadCallback uploadCallback) {
		return internalExecute(new StringResponseBody(), nioCallback, null, httpCallback, uploadCallback);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> execute(HttpCallback<T> httpCallback, ResponseBodyConsumer<T> responseBodyConsumer, UploadCallback uploadCallback) {
		return internalExecute(responseBodyConsumer, null, null, httpCallback, uploadCallback);

	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> execute(ResponseBodyConsumer<T> responseBodyConsumer, UploadCallback uploadCallback) {
		return internalExecute(responseBodyConsumer, null, null, null, uploadCallback);
	}

	@Override
	public CompletableFuture<HttpResponse<String>> execute(NioCallback nioCallback, UploadCallback uploadCallback) {
		return internalExecute(new StringResponseBody(), nioCallback, null, null, uploadCallback);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> execute(HttpCallback<T> httpCallback, ResponseBodyConsumer<T> responseBodyConsumer, NioCallback nioCallback,
														  UploadCallback uploadCallback) {
		return internalExecute(responseBodyConsumer, nioCallback, null, httpCallback, uploadCallback);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> execute(HttpCallback<T> httpCallback, ResponseBodyConsumer<T> responseBodyConsumer, NioCallback nioCallback,
														  ExternalEventTrigger externalEventTrigger, UploadCallback uploadCallback) {
		return internalExecute(responseBodyConsumer, nioCallback, externalEventTrigger, httpCallback, uploadCallback);
	}
}
