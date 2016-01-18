// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface HttpClientRequestBuilder {

	HttpClientRequestBuilder withHeader(String name, String value);

	HttpClientRequestBuilder keepAlive(boolean keepAlive);

	HttpClientRequestBuilder acceptCompressedResponse(boolean acceptCompressedResponse);

	HttpClientRequestBuilder withQueryParameter(String name, String value);

	HttpClientRequestBuilder idleTimeoutMillis(int readTimeoutMillis);

	HttpClientRequestBuilder totalRequestTimeoutMillis(int requestTimeoutMillis);

	HttpClientRequestBuilder followRedirects(boolean followRedirects);

	BuiltClientRequest build();


}
