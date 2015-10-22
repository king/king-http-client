// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface HttpClientRequest {

	HttpClientRequest withHeader(String name, String value);

	HttpClientRequest keepAlive(boolean keepAlive);

	HttpClientRequest acceptCompressedResponse(boolean acceptCompressedResponse);

	HttpClientRequest withQueryParameter(String name, String value);

	HttpClientRequest idleTimeoutMillis(int readTimeoutMillis);

	HttpClientRequest totalRequestTimeoutMillis(int requestTimeoutMillis);

	HttpClientRequest followRedirects(boolean followRedirects);

	BuiltClientRequest build();


}
