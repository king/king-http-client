// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.function.Supplier;

public interface HttpClientRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientRequestBuilder> {

	/**
	 * Build the request. The response is consumed as String.
	 * @return the built request
	 */
	BuiltClientRequest<String> build();

	/**
	 * Build the request. The response is consumed through the responseBodyConsumer
	 * @param responseBodyConsumer the consumer for this request
	 * @param <T> the type returned by the completed responseBodyConsumer
	 * @return the build request
	 */
	<T> BuiltClientRequest<T> build(Supplier<ResponseBodyConsumer<T>> responseBodyConsumer);

}
