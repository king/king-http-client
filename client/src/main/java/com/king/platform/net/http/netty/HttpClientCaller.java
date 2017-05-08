// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.NioCallback;
import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.request.NettyHttpClientRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public interface HttpClientCaller {
	<T> Future<FutureResult<T>> execute(HttpMethod httpMethod, NettyHttpClientRequest<T> nettyHttpClientRequest, HttpCallback<T> httpCallback,
										NioCallback nioCallback, ResponseBodyConsumer<T> responseBodyConsumer, int idleTimeoutMillis,
										int totalRequestTimeoutMillis, boolean followRedirects, boolean keepAlive,
										ExternalEventTrigger externalEventTrigger, Executor callbackExecutor);

}
