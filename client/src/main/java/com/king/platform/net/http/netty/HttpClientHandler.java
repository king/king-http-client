// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import com.king.platform.net.http.netty.response.HttpClientResponseHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Sharable
public class HttpClientHandler extends BaseHttpRequestHandler {

	public static final AttributeKey<Boolean> HTTP_CLIENT_HANDLER_TRIGGERED_ERROR = AttributeKey.valueOf("__HttpClientHandler_ErrorTriggered");

	private final Logger logger = getLogger(getClass());


	public HttpClientHandler(HttpClientResponseHandler httpClientResponseHandler, HttpClientRequestHandler httpClientRequestHandler) {
		super(httpClientResponseHandler, httpClientRequestHandler);
	}

}
