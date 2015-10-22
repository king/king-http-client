// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.concurrent.Future;

public interface BuiltClientRequest {
	Future<FutureResult<String>> execute();

	<T> Future<FutureResult<T>> execute(HttpCallback<T> httpCallback);

	<T> Future<FutureResult<T>> execute(ResponseBodyConsumer<T> responseBodyConsumer);

	Future<FutureResult<String>> execute(NioCallback nioCallback);

	<T> Future<FutureResult<T>> execute(HttpCallback<T> httpCallback, NioCallback nioCallback);
}
