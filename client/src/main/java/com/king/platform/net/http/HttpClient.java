// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface HttpClient {
	void start();

	void shutdown();

	<T> void setConf(ConfKeys<T> key, T value);

	HttpClientRequestBuilder createGet(String uri);

	HttpClientRequestWithBodyBuilder createPost(String uri);

	HttpClientRequestWithBodyBuilder createPut(String uri);

	HttpClientRequestBuilder createDelete(String uri);


}
