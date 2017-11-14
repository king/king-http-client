package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.netty.BaseHttpRequestHandler;
import com.king.platform.net.http.netty.request.HttpClientRequestHandler;
import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class WebSocketHandler extends BaseHttpRequestHandler {
	public WebSocketHandler(WebSocketResponseHandler webSocketResponseHandler, HttpClientRequestHandler httpClientRequestHandler) {
		super(webSocketResponseHandler, httpClientRequestHandler);
	}
}
