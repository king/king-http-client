// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;

import com.king.platform.net.http.netty.request.HttpBody;
import com.king.platform.net.http.netty.request.InputStreamHttpBody;

import java.io.InputStream;
import java.nio.charset.Charset;

class InputStreamHttpBodyBuilder implements RequestBodyBuilder {
	private final InputStream inputStream;

	public InputStreamHttpBodyBuilder(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Override
	public HttpBody createHttpBody(String contentType, Charset characterEncoding, boolean isSecure) {
		return new InputStreamHttpBody(inputStream, contentType, characterEncoding);
	}
}
