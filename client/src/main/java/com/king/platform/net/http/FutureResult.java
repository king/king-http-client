// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

public class FutureResult<T> {
	private HttpResponse<T> httpResponse;
	private Throwable error;

	public FutureResult(HttpResponse<T> httpResponse) {
		this.httpResponse = httpResponse;
	}


	public FutureResult(Throwable error) {
		this.error = error;
	}

	public HttpResponse<T> getHttpResponse() {
		return httpResponse;
	}

	public Throwable getError() {
		return error;
	}


	public boolean completed() {
		return httpResponse != null;
	}

	public boolean failed() {
		return error != null;
	}

}
