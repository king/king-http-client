// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.nio.ByteBuffer;

public interface ResponseBodyConsumer<T> {
	void onBodyStart(String contentType, String charset, long contentLength) throws Exception;

	void onReceivedContentPart(ByteBuffer buffer) throws Exception;

	void onCompletedBody() throws Exception;

	T getBody();

}
