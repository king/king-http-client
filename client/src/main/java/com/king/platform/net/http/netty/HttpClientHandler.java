// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import com.king.platform.net.http.netty.response.HttpClientResponseHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Sharable
public class HttpClientHandler extends ChannelDuplexHandler {

	public static final AttributeKey<Boolean> HTTP_CLIENT_HANDLER_TRIGGERED_ERROR = AttributeKey.valueOf("__HttpClientHandler_ErrorTriggered");

	private final Logger logger = getLogger(getClass());
	private final HttpClientResponseHandler httpClientResponseHandler;
	private final HttpClientRequestHandler httpClientRequestHandler;


	public HttpClientHandler(HttpClientResponseHandler httpClientResponseHandler, HttpClientRequestHandler httpClientRequestHandler) {
		this.httpClientResponseHandler = httpClientResponseHandler;
		this.httpClientRequestHandler = httpClientRequestHandler;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		httpClientResponseHandler.handleResponse(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		httpClientRequestHandler.handleRequest(ctx, msg, promise);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		httpClientResponseHandler.handleChannelInactive(ctx);
		super.channelReadComplete(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) {
		logger.trace("Exception on channel " + ctx.channel(), cause);

		ctx.channel().attr(HTTP_CLIENT_HANDLER_TRIGGERED_ERROR).set(true);

		HttpRequestContext httpRequestContext = ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).get();
		if (httpRequestContext != null) {
			httpRequestContext.getRequestEventBus().triggerEvent(Event.ERROR, httpRequestContext, cause);
		}
	}

}
