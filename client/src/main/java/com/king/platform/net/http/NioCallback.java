// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public interface NioCallback {
	void onConnecting();

	void onConnected();

	void onWroteHeaders();

	void onWroteContentProgressed(long progress, long total);

	void onWroteContentCompleted();

	void onReceivedStatus(HttpResponseStatus httpResponseStatus);

	void onReceivedHeaders(HttpHeaders httpHeaders);

	void onReceivedContentPart(int len, ByteBuf buffer);

	void onReceivedCompleted(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders);

	void onError(Throwable throwable);

}
