// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.request.NettyHttpClientRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface HttpClientCaller {
	<T> CompletableFuture<HttpResponse<T>> execute(HttpMethod httpMethod, NettyHttpClientRequest<T> nettyHttpClientRequest, HttpCallback<T> httpCallback,
												   NioCallback nioCallback, UploadCallback uploadCallback, ResponseBodyConsumer<T> responseBodyConsumer,
												   Executor callbackExecutor, ExternalEventTrigger externalEventTrigger, CustomCallbackSubscriber customCallbackSubscriber, int idleTimeoutMillis,
												   int totalRequestTimeoutMillis, boolean followRedirects, boolean keepAlive, boolean
													   automaticallyDecompressResponse);

}
