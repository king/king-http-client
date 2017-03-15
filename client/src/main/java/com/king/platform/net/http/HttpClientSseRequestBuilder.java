// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.concurrent.Executor;

public interface HttpClientSseRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientSseRequestBuilder>  {
	/**
	 * Provide a custom executor that will be used for the sse request.
	 * For example an single threaded executor can be used to guarantee the order of the events.
	 * @param executor the executor
	 * @return the builder
	 */
	HttpClientSseRequestBuilder executingOn(Executor executor);

	/**
	 * Build the request
	 * @return the built request
	 */
	BuiltSseClientRequest build();

}
