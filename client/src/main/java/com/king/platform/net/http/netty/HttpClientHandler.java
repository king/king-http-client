// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import com.king.platform.net.http.netty.response.HttpClientResponseHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class HttpClientHandler extends BaseHttpRequestHandler {

	public HttpClientHandler(HttpClientResponseHandler httpClientResponseHandler, HttpClientRequestHandler httpClientRequestHandler) {
		super(httpClientResponseHandler, httpClientRequestHandler);
	}

}
