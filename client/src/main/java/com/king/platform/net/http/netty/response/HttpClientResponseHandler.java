// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.response;


import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.HttpClientHandler;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpClientResponseHandler {

	private final Logger logger = getLogger(getClass());
	private final HttpRedirector httpRedirector;

	public HttpClientResponseHandler(HttpRedirector httpRedirector) {
		this.httpRedirector = httpRedirector;
	}

	public void handleResponse(ChannelHandlerContext ctx, Object msg) throws Exception {
		HttpRequestContext httpRequestContext = ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).get();

		if (httpRequestContext == null) {
			logger.trace("httpRequestContext is null, msg was {}", msg);
			return;
		}

		if (ctx.channel().attr(HttpClientHandler.HTTP_CLIENT_HANDLER_TRIGGERED_ERROR).get()) {
			logger.trace("This channel has already triggered error, ignoring this invocation");
			return;
		}


		NettyHttpClientResponse nettyHttpClientResponse = httpRequestContext.getNettyHttpClientResponse();

		RequestEventBus requestEventBus = nettyHttpClientResponse.getRequestEventBus();

		ResponseBodyConsumer responseBodyConsumer = nettyHttpClientResponse.getResponseBodyConsumer();

		try {

			if (msg instanceof HttpResponse) {
				requestEventBus.triggerEvent(Event.TOUCH);

				logger.trace("read HttpResponse");
				HttpResponse response = (HttpResponse) msg;

				HttpResponseStatus httpResponseStatus = response.status();
				HttpHeaders httpHeaders = response.headers();

				nettyHttpClientResponse.setHttpResponseStatus(httpResponseStatus);
				nettyHttpClientResponse.setHttpHeaders(httpHeaders);

				requestEventBus.triggerEvent(Event.onReceivedStatus, httpResponseStatus);
				requestEventBus.triggerEvent(Event.onReceivedHeaders, httpHeaders);

				httpRequestContext.getTimeRecorder().readResponseHttpHeaders();

				if (httpRequestContext.isFollowRedirects() && httpRedirector.isRedirectResponse(httpResponseStatus)) {
					httpRedirector.redirectRequest(httpRequestContext, httpHeaders);
					return;
				}


				if (response.status().code() == 100) {
					requestEventBus.triggerEvent(Event.WRITE_BODY, ctx);
					return;
				}


				String contentLength = httpHeaders.get(HttpHeaderNames.CONTENT_LENGTH);

				String contentType = httpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
				String charset = StringUtil.substringAfter(contentType, '=');
				if (charset == null) {
					charset = StandardCharsets.ISO_8859_1.name();
				}

				contentType = StringUtil.substringBefore(contentType, ';');

				if (contentLength != null) {
					long length = Long.parseLong(contentLength);
					responseBodyConsumer.onBodyStart(contentType, charset, length);
				} else {
					responseBodyConsumer.onBodyStart(contentType, charset, 0);
				}

				httpRequestContext.getTimeRecorder().responseBodyStart();

			} else if (msg instanceof HttpContent) {
				logger.trace("read HttpContent");
				requestEventBus.triggerEvent(Event.TOUCH);


				HttpResponseStatus httpResponseStatus = nettyHttpClientResponse.getHttpResponseStatus();
				HttpHeaders httpHeaders = nettyHttpClientResponse.getHttpHeaders();

				if (httpResponseStatus == null || (httpRequestContext.isFollowRedirects() && httpRedirector.isRedirectResponse(httpResponseStatus))) {
					return;
				}

				if (msg == LastHttpContent.EMPTY_LAST_CONTENT && nettyHttpClientResponse.getHttpResponseStatus().code() == 100) {
					logger.trace("read EMPTY_LAST_CONTENT with status code 100");
					return;
				}


				HttpContent chunk = (HttpContent) msg;

				ByteBuf content = chunk.content();

				content.resetReaderIndex();

				int readableBytes = content.readableBytes();

				if (readableBytes > 0) {
					ByteBuffer byteBuffer = content.nioBuffer();

					responseBodyConsumer.onReceivedContentPart(byteBuffer);
					requestEventBus.triggerEvent(Event.onReceivedContentPart, readableBytes, content);

				}


				content.release();

				requestEventBus.triggerEvent(Event.TOUCH);


				if (chunk instanceof LastHttpContent) {


					responseBodyConsumer.onCompletedBody();

					requestEventBus.triggerEvent(Event.onReceivedCompleted, httpResponseStatus, httpHeaders);
					httpRequestContext.getTimeRecorder().responseBodyCompleted();

					@SuppressWarnings("unchecked")
					com.king.platform.net.http.HttpResponse httpResponse = new com.king.platform.net.http.HttpResponse(httpResponseStatus.code(),
						responseBodyConsumer, httpHeaders.entries());


					requestEventBus.triggerEvent(Event.onHttpResponseDone, httpResponse);

					requestEventBus.triggerEvent(Event.COMPLETED, httpRequestContext);


				}
			}
		} catch (Throwable e) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, e);
		}
	}


}
