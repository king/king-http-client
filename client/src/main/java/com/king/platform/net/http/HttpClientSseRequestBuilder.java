// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface HttpClientSseRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientSseRequestBuilder> {
	/**
	 * Build the request
	 *
	 * @return the built request
	 */
	BuiltSseClientRequest build();

}
