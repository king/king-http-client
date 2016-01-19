// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.concurrent.Future;

public interface BuiltClientRequest {

	/**
	 * Execute the built request, consume the returned data as String
	 *
	 * @return a future with the result of the request
	 */
	Future<FutureResult<String>> execute();


	/**
	 * Execute the built request, consume the returned data as String
	 *
	 * @param httpCallback the callback object, executed on the HttpCallbackExecutor
	 * @return a future with the result of the request
	 */
	Future<FutureResult<String>> execute(HttpCallback<String> httpCallback);


	/**
	 * Execute the built request, consume the returned data as String
	 *
	 * @param httpCallback the callback object, executed on the HttpCallbackExecutor
	 * @param nioCallback  the NioCallback object, executed on the io thread for this request.
	 * @return a future with the result of the request
	 */
	Future<FutureResult<String>> execute(HttpCallback<String> httpCallback, NioCallback nioCallback);


	/**
	 * Execute the built request, consume the returned data as the type defined by responseBodyConsumer
	 *
	 * @param httpCallback         the callback object, executed on the HttpCallbackExecutor
	 * @param responseBodyConsumer the consumer of the read http body
	 * @param <T>                  the type defined by the responseBodyConsumer
	 * @return a future with the result of the request
	 */
	<T> Future<FutureResult<T>> execute(HttpCallback<T> httpCallback, ResponseBodyConsumer<T> responseBodyConsumer);

	/**
	 * Execute the built request, consume the returned data as the type defined by the responseBodyConsumer parameter
	 *
	 * @param responseBodyConsumer the consumer of the response body
	 * @param <T>                  the type of the response body
	 * @return a future with the result of the request
	 */
	<T> Future<FutureResult<T>> execute(ResponseBodyConsumer<T> responseBodyConsumer);

	/**
	 * Execute the built request, consume the returned data as String
	 *
	 * @param nioCallback the NioCallback object, executed on the io thread for this request.
	 * @return a future with the result of the request
	 */
	Future<FutureResult<String>> execute(NioCallback nioCallback);

	/**
	 * Execute the built request, consume the returned data as the type defined by responseBodyConsumer
	 *
	 * @param httpCallback         the callback object, executed on the HttpCallbackExecutor
	 * @param responseBodyConsumer the consumer of the read http body
	 * @param nioCallback          the NioCallback object, executed on the io thread for this request.
	 * @param <T>                  the type defined by the responseBodyConsumer
	 * @return a future with the result of the request
	 */
	<T> Future<FutureResult<T>> execute(HttpCallback<T> httpCallback, ResponseBodyConsumer<T> responseBodyConsumer, NioCallback nioCallback);
}
