package com.king.platform.net.http;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.concurrent.CompletableFuture;

public interface BuiltWebSocketRequest {
	/**
	 * Builds and executes (connects) the web-socket connection.
	 * @param webSocketListener the listener
	 * @return the future with the connected webSocketConnection
	 */
	CompletableFuture<WebSocketClient> execute(WebSocketListener webSocketListener);

	/**
	 * Builds and executes (connects) the web-socket connection.
	 * @param webSocketFrameListener the listener
	 * @return the future with the connected webSocketConnection
	 */
	CompletableFuture<WebSocketClient> execute(WebSocketFrameListener webSocketFrameListener);

	/**
	 * Builds and executes (connects) the web-socket connection.
	 * @param webSocketMessageListener the listener
	 * @return the future with the connected webSocketConnection
	 */
	CompletableFuture<WebSocketClient> execute(WebSocketMessageListener webSocketMessageListener);


	/**
	 * Builds a web-socket connection that is not yet connected.
	 * It can later be connected by calling {@link WebSocketClient#connect()}
	 * @return the not yet connected web-socket
	 */
	WebSocketClient build();
}
