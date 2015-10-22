// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.response;


import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class NettyHttpClientResponse<T> {

	private final ResponseBodyConsumer<T> responseBodyConsumer;
	private final RequestEventBus requestEventBus;
	private HttpResponseStatus httpResponseStatus;
	private HttpHeaders httpHeaders;

	public NettyHttpClientResponse(ResponseBodyConsumer<T> responseBodyConsumer, RequestEventBus requestEventBus) {
		this.responseBodyConsumer = responseBodyConsumer;
		this.requestEventBus = requestEventBus;
	}


	public HttpResponseStatus getHttpResponseStatus() {
		return httpResponseStatus;
	}

	public void setHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
		this.httpResponseStatus = httpResponseStatus;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public ResponseBodyConsumer<T> getResponseBodyConsumer() {
		return responseBodyConsumer;
	}


	public RequestEventBus getRequestEventBus() {
		return requestEventBus;
	}
}
