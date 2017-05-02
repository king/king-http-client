// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;

import com.king.platform.net.http.netty.request.ByteArrayHttpBody;
import com.king.platform.net.http.netty.request.HttpBody;

import java.nio.charset.Charset;

class ByteArrayHttpBodyBuilder implements RequestBodyBuilder {
	private final byte[] content;

	ByteArrayHttpBodyBuilder(byte[] content) {
		this.content = content;
	}

	@Override
	public HttpBody createHttpBody(String contentType, Charset characterEncoding, boolean isSecure) {
		return new ByteArrayHttpBody(content, contentType, characterEncoding);
	}
}
