// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface HttpClientRequestHeaderBuilder<T> {

	T withHeader(String name, String value);

	T keepAlive(boolean keepAlive);

	T acceptCompressedResponse(boolean acceptCompressedResponse);

	T withQueryParameter(String name, String value);

	T idleTimeoutMillis(int readTimeoutMillis);

	T totalRequestTimeoutMillis(int requestTimeoutMillis);

	T followRedirects(boolean followRedirects);

}
