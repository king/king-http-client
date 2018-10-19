// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;
import java.util.Map;

public class HttpResponse<T> {

	private final HttpVersion httpVersion;
	private final Headers headers;
	private final HttpResponseStatus status;
	private final ResponseBodyConsumer<T> responseBodyConsumer;


	public HttpResponse(HttpVersion httpVersion, HttpResponseStatus status,
						ResponseBodyConsumer responseBodyConsumer, HttpHeaders httpHeaders) {
		this.httpVersion = httpVersion;
		this.status = status;
		this.responseBodyConsumer = responseBodyConsumer;
		this.headers = new Headers(httpHeaders);
	}

	public String getHttpVersion() {
		return httpVersion.toString();
	}

	public int getStatusCode() {
		return status.code();
	}

	public String getStatusReason() {
		return status.reasonPhrase();
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
