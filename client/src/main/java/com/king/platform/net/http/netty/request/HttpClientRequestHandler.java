// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;


import com.king.platform.net.http.netty.HttpClientHandler;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.eventbus.RunOnceCallback1;
import com.king.platform.net.http.netty.response.NettyHttpClientResponse;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpClientRequestHandler {

	private final Logger logger = getLogger(getClass());

	public HttpClientRequestHandler() {

	}

	public void handleRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		Attribute<Boolean> errorAttribute = ctx.channel().attr(HttpClientHandler.HTTP_CLIENT_HANDLER_TRIGGERED_ERROR);
		if (errorAttribute.get() != null && errorAttribute.get()) {
			logger.trace("This channel has already triggered error, ignoring this invocation");
			return;
		}

		if (HttpRequestContext.class.isAssignableFrom(msg.getClass())) {
			errorAttribute.set(false);
			HttpRequestContext httpRequestContext = (HttpRequestContext) msg;

			NettyHttpClientRequest request = httpRequestContext.getNettyHttpClientRequest();

			ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).set(httpRequestContext);

			RequestEventBus requestEventBus = httpRequestContext.getRequestEventBus();

			NettyHttpClientResponse nettyHttpClientResponse = new NettyHttpClientResponse(httpRequestContext.getResponseBodyConsumer(), requestEventBus);
			httpRequestContext.setNettyHttpClientResponse(nettyHttpClientResponse);


			writeHeaders(ctx, httpRequestContext, request.getNettyRequest(), requestEventBus);

			if (request.isDontWriteBodyBecauseExpectContinue()) {
				requestEventBus.subscribe(Event.WRITE_BODY, new RunOnceCallback1<ChannelHandlerContext>() {
					@Override
					public void onFirstEvent(Event1 event, ChannelHandlerContext ctx) {
						HttpRequestContext httpRequestContext = ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).get();
						NettyHttpClientRequest request = httpRequestContext.getNettyHttpClientRequest();
						logger.trace("DelayedBodyWriter writing body");
						writeHttpBody(ctx, httpRequestContext, request.getHttpBody(), httpRequestContext.getRequestEventBus());
						ctx.channel().flush();
					}
				});

			} else {

				if (request.getHttpBody() != null) {

					writeHttpBody(ctx, httpRequestContext, request.getHttpBody(), requestEventBus);
				} else {
					writeLastHttpContent(ctx, httpRequestContext, requestEventBus);
				}
			}
		} else {
			ctx.write(msg, promise).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						logger.error("Failed to write unknown message", future.cause());
					}
				}
			});
		}
	}


	private void writeHeaders(ChannelHandlerContext ctx, final HttpRequestContext httpRequestContext, HttpRequest httpRequest, final RequestEventBus
		requestEventBus) {
		httpRequestContext.getTimeRecorder().startWriteHeaders();
		ChannelFuture channelFuture = ctx.write(httpRequest);
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.trace("Wrote headers operation completed, future: {}", future);
					requestEventBus.triggerEvent(Event.onWroteHeaders);
					requestEventBus.triggerEvent(Event.TOUCH);
					httpRequestContext.getTimeRecorder().completedWriteHeaders();

				} else {
					requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, future.cause());
				}

			}
		});
	}

	private void writeHttpBody(final ChannelHandlerContext ctx, final HttpRequestContext httpRequestContext, HttpBody httpBody, final RequestEventBus
		requestEventBus) {
		try {
			httpRequestContext.getTimeRecorder().startWriteBody();
			requestEventBus.triggerEvent(Event.onWroteContentStarted, httpBody.getContentLength());
			ChannelFuture channelFuture = httpBody.writeContent(ctx);

			channelFuture.addListener(new ChannelProgressiveFutureListener() {
				@Override
				public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
					requestEventBus.triggerEvent(Event.TOUCH);
					requestEventBus.triggerEvent(Event.onWroteContentProgressed, progress, total);

				}

				@Override
				public void operationComplete(ChannelProgressiveFuture future) throws Exception {
					logger.trace("Wrote content operation completed, future: {}", future);

					if (future.isSuccess()) {
						httpRequestContext.getTimeRecorder().completedWriteBody();
						writeLastHttpContent(ctx, httpRequestContext, requestEventBus);
						requestEventBus.triggerEvent(Event.TOUCH);


					} else {
						logger.error("Failed to write http body, future: " + future);
						requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, future.cause());
					}
				}
			});

		} catch (IOException e) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new IOException("Failed to write body to server", e));
		}


	}

	private void writeLastHttpContent(ChannelHandlerContext ctx, final HttpRequestContext httpRequestContext, final RequestEventBus requestEventBus) {
		ChannelFuture future = ctx.writeAndFlush(new DefaultLastHttpContent());
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				logger.trace("writeLastHttpContent operation completed, future: {}", future);

				if (future.isSuccess()) {
					requestEventBus.triggerEvent(Event.onWroteContentCompleted);
					requestEventBus.triggerEvent(Event.TOUCH);
					httpRequestContext.getTimeRecorder().completedWriteLastBody();

				} else {
					requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, future.cause());
				}

			}
		});
	}


}
