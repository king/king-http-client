// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;
import java.util.Map;

public class HttpResponse<T> {
	private Headers headers;
	private final int statusCode;
	private final ResponseBodyConsumer<T> responseBodyConsumer;


	public HttpResponse(int statusCode, ResponseBodyConsumer responseBodyConsumer, HttpHeaders httpHeaders) {
		this.statusCode = statusCode;
		this.responseBodyConsumer = responseBodyConsumer;
		headers = new Headers(httpHeaders);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public T getBody() {
		return responseBodyConsumer.getBody();
	}

	public String getHeader(CharSequence name) {
		return headers.get(name);
	}

	public List<String> getHeaders(CharSequence name) {

		return headers.getAll(name);
	}

	public List<Map.Entry<String, String>> getAllHeaders() {
		return headers.entries();
	}
}
