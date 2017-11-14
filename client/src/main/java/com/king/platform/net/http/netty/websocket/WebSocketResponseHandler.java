package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.WebSocketClient;
import com.king.platform.net.http.netty.HttpClientHandler;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ResponseHandler;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.response.NettyHttpClientResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;

import static io.netty.handler.codec.http.HttpHeaderNames.SEC_WEBSOCKET_ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.SEC_WEBSOCKET_KEY;
import static org.slf4j.LoggerFactory.getLogger;

public class WebSocketResponseHandler implements ResponseHandler {
	private final Logger logger = getLogger(getClass());

	@Override
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

		requestEventBus.triggerEvent(Event.TOUCH);

		if (msg instanceof HttpResponse) {

			HttpResponse response = (HttpResponse) msg;

			if (response.status() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
				upgrade(response, requestEventBus, ctx.channel(), httpRequestContext);
			} else {
				abort(requestEventBus, httpRequestContext);
			}


		} else if (msg instanceof WebSocketFrame) {

			WebSocketFrame frame = (WebSocketFrame) msg;
			handleFrame(frame, requestEventBus);
		} else {
			logger.error("Invalid message {}", msg);
		}
	}

	public void handleFrame(WebSocketFrame frame, RequestEventBus requestEventBus) {
		if (frame instanceof TextWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsTextFrame, (TextWebSocketFrame) frame);

		} else if (frame instanceof BinaryWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsBinaryFrame, (BinaryWebSocketFrame) frame);

		} else if (frame instanceof CloseWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsCloseFrame, (CloseWebSocketFrame) frame);

		} else if (frame instanceof PingWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsPingFrame, (PingWebSocketFrame) frame);

		} else if (frame instanceof PongWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsPongFrame, (PongWebSocketFrame) frame);

		} else if (frame instanceof ContinuationWebSocketFrame) {
			requestEventBus.triggerEvent(Event.onWsContinuationFrame, (ContinuationWebSocketFrame) frame);
		} else {
			logger.error("Invalid message {}", frame);

		}
	}

	private void abort(RequestEventBus requestEventBus, HttpRequestContext httpRequestContext) {
		requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new WebSocketException("Invalid upgrade - aborting!"));
	}

	private void upgrade(HttpResponse response, RequestEventBus requestEventBus, Channel channel, HttpRequestContext httpRequestContext) {

		boolean validStatus = response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS);
		boolean validUpgrade = response.headers().get(HttpHeaderNames.UPGRADE) != null;
		String connection = response.headers().get(HttpHeaderNames.CONNECTION);

		boolean validConnection = HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(connection);

		if (!validStatus || !validUpgrade || !validConnection) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new WebSocketException("Invalid handshake!"));
			return;
		}


		String receivedKey = response.headers().get(SEC_WEBSOCKET_ACCEPT);
		String sentKey = httpRequestContext.getNettyHttpClientRequest().getNettyHeaders().get(SEC_WEBSOCKET_KEY);
		String expectedKey = WebSocketUtil.getAcceptKey(sentKey);
		if (receivedKey == null || !expectedKey.equals(receivedKey)) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new WebSocketException("Invalid Challenge! Received key: " + receivedKey + ", Expected key: " + expectedKey));
			return;
		}


		requestEventBus.triggerEvent(Event.WS_UPGRADE_PIPELINE, channel.pipeline());

		requestEventBus.triggerEvent(Event.onWsOpen, channel, response.headers());

	}

	@Override
	public void handleChannelInactive(ChannelHandlerContext ctx) throws Exception {

	}
}
