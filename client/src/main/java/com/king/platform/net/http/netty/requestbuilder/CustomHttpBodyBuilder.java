// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;

import com.king.platform.net.http.netty.request.HttpBody;

import java.nio.charset.Charset;

class CustomHttpBodyBuilder implements RequestBodyBuilder {
	private final HttpBody httpBody;

	public CustomHttpBodyBuilder(HttpBody httpBody) {
		this.httpBody = httpBody;
	}

	@Override
	public String getName() {
		return "Custom Http Body";
	}

	@Override
	public HttpBody createHttpBody(String contentType, Charset charset, boolean isSecure) {
		return httpBody;
	}
}
