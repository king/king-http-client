// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.response;


import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.BaseHttpRequestHandler;
import com.king.platform.net.http.netty.ConnectionClosedException;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ResponseHandler;
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

public class HttpClientResponseHandler implements ResponseHandler {

	private final Logger logger = getLogger(getClass());
	private final HttpRedirector httpRedirector;

	public HttpClientResponseHandler(HttpRedirector httpRedirector) {
		this.httpRedirector = httpRedirector;
	}

	@Override
	public void handleResponse(ChannelHandlerContext ctx, Object msg) throws Exception {
		HttpRequestContext httpRequestContext = ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).get();

		if (httpRequestContext == null) {
			logger.trace("httpRequestContext is null, msg was {}", msg);
			return;
		}

		if (ctx.channel().attr(BaseHttpRequestHandler.HTTP_CLIENT_HANDLER_TRIGGERED_ERROR).get()) {
			logger.trace("This channel has already triggered error, ignoring this invocation");
			return;
		}

		if (httpRequestContext.hasCompletedContent()) {
			logger.trace("This request has already been processed completely, ignoring the rest");
			return;
		}

		NettyHttpClientResponse nettyHttpClientResponse = httpRequestContext.getNettyHttpClientResponse();

		RequestEventBus requestEventBus = nettyHttpClientResponse.getRequestEventBus();

		ResponseBodyConsumer responseBodyConsumer = nettyHttpClientResponse.getResponseBodyConsumer();


		try {

			if (msg instanceof HttpResponse) {

				if (((HttpResponse) msg).decoderResult().isFailure()) {
					logger.trace("Got invalid data from server: {}", msg);
					requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new IllegalStateException("Invalid http data from server! " + msg));
					return;
				}

				requestEventBus.triggerEvent(Event.TOUCH);

				logger.trace("read HttpResponse {}", msg);
				HttpResponse response = (HttpResponse) msg;

				HttpResponseStatus httpResponseStatus = response.status();
				HttpHeaders httpHeaders = response.headers();

				nettyHttpClientResponse.setHttpResponseStatus(httpResponseStatus);
				nettyHttpClientResponse.setHttpHeaders(httpHeaders);

				requestEventBus.triggerEvent(Event.onReceivedStatus, httpResponseStatus);
				requestEventBus.triggerEvent(Event.onReceivedHeaders, httpHeaders);

				httpRequestContext.getTimeRecorder().readResponseHttpHeaders();

				if (httpRequestContext.isFollowRedirects() && HttpRedirector.isRedirectResponse(httpResponseStatus)) {
					httpRedirector.redirectRequest(httpRequestContext, httpHeaders);
					return;
				}

				if (httpRequestContext.getHttpMethod().equals(HttpMethod.HEAD)) {
					httpRequestContext.getTimeRecorder().responseBodyStart();
					httpRequestContext.getTimeRecorder().responseBodyCompleted();
					return;
				}

				if (response.status().code() == 100) {
					requestEventBus.triggerEvent(Event.WRITE_BODY, ctx);
					return;
				}


				String contentLength = httpHeaders.get(HttpHeaderNames.CONTENT_LENGTH);

				String contentType = httpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
				String charset = StringUtil.substringAfter(contentType, '=', true);
				if (charset == null) {
					charset = StandardCharsets.ISO_8859_1.name();
				}

				contentType = StringUtil.substringBefore(contentType, ';');

				if (contentLength != null) {
					long length = Long.parseLong(contentLength);
					httpRequestContext.setExpectedContentLength(length);
					responseBodyConsumer.onBodyStart(contentType, charset, length);
				} else {
					responseBodyConsumer.onBodyStart(contentType, charset, 0);
				}

				httpRequestContext.getTimeRecorder().responseBodyStart();

			} else if (msg instanceof HttpContent) {
				logger.trace("read HttpContent");
				requestEventBus.triggerEvent(Event.TOUCH);


				HttpResponseStatus httpResponseStatus = nettyHttpClientResponse.getHttpResponseStatus();

				if (httpResponseStatus == null || (httpRequestContext.isFollowRedirects() && HttpRedirector.isRedirectResponse(httpResponseStatus))) {
					httpRequestContext.setRedirecting(true);
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
					httpRequestContext.addReadBytes(readableBytes);
				}


				content.release();

				requestEventBus.triggerEvent(Event.TOUCH);


				if (chunk instanceof LastHttpContent) {
					if (incomnpleteReadOfData(httpRequestContext)) {
						triggerServerClosedException(httpRequestContext, requestEventBus, "Connection closed before all response data was read!");
						return;
					}
					logger.trace("Got LastHttpContent, completing request");
					handleCompletedTransfer(httpRequestContext, requestEventBus, nettyHttpClientResponse);
				}
			}
		} catch (Throwable e) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, e);
		}
	}

	private void handleCompletedTransfer(HttpRequestContext httpRequestContext, RequestEventBus requestEventBus, NettyHttpClientResponse
		nettyHttpClientResponse) throws Exception {
		ResponseBodyConsumer responseBodyConsumer = nettyHttpClientResponse.getResponseBodyConsumer();
		HttpResponseStatus httpResponseStatus = nettyHttpClientResponse.getHttpResponseStatus();
		HttpHeaders httpHeaders = nettyHttpClientResponse.getHttpHeaders();

		httpRequestContext.setHasCompleted(true);

		responseBodyConsumer.onCompletedBody();

		requestEventBus.triggerEvent(Event.onReceivedCompleted, httpResponseStatus, httpHeaders);
		httpRequestContext.getTimeRecorder().responseBodyCompleted();

		@SuppressWarnings("unchecked") com.king.platform.net.http.HttpResponse httpResponse =
			new com.king.platform.net.http.HttpResponse(httpVersion(httpRequestContext), httpResponseStatus, responseBodyConsumer, httpHeaders);

		requestEventBus.triggerEvent(Event.onHttpResponseDone, httpResponse);

		requestEventBus.triggerEvent(Event.COMPLETED, httpRequestContext);
	}


	@Override
	public void handleChannelInactive(ChannelHandlerContext ctx) {
		logger.trace("Channel {} became inactive", ctx.channel());
		HttpRequestContext httpRequestContext = ctx.channel().attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY).get();

		if (httpRequestContext == null) {
			return;
		}

		if (ctx.channel().attr(BaseHttpRequestHandler.HTTP_CLIENT_HANDLER_TRIGGERED_ERROR).get()) {
			return;
		}

		if (httpRequestContext.hasCompletedContent()) {
			return;
		}

		if (httpRequestContext.isRedirecting()) {
			return;
		}


		NettyHttpClientResponse nettyHttpClientResponse = httpRequestContext.getNettyHttpClientResponse();
		RequestEventBus requestEventBus = nettyHttpClientResponse.getRequestEventBus();

		if (nettyHttpClientResponse.getHttpResponseStatus() == null || nettyHttpClientResponse.getHttpHeaders() == null) {  //the connection has closed before response headers has been read
			triggerServerClosedException(httpRequestContext, requestEventBus, "Connection closed before response http headers was read!");
			return;
		}


		if (incomnpleteReadOfData(httpRequestContext)) {  //the connection has closed before all body was read
			triggerServerClosedException(httpRequestContext, requestEventBus, "Connection closed before all response data was read!");
			return;
		}

		try {
			logger.trace("Completing the pending request due to channel {} became idle", ctx.channel());
			handleCompletedTransfer(httpRequestContext, requestEventBus, nettyHttpClientResponse);
		} catch (Exception e) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, e);
		}

	}

	private static HttpVersion httpVersion(final HttpRequestContext httpRequestContext) {
		return httpRequestContext.getNettyHttpClientRequest().getNettyRequest().protocolVersion();
	}

	private boolean incomnpleteReadOfData(HttpRequestContext httpRequestContext) {
		return httpRequestContext.getExpectedContentLength() > 0 && httpRequestContext.getReadBytes() != httpRequestContext.getExpectedContentLength();
	}

	private void triggerServerClosedException(HttpRequestContext httpRequestContext, RequestEventBus requestEventBus, String message) {
		requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new ConnectionClosedException(message));
	}
}
