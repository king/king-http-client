package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface BuiltWebSocketRequest {
	/**
	 * Builds and executes (connects) the web-socket connection.
	 * @param webSocketListener the listener
	 * @return the future with the connected webSocketConnection
	 */
	CompletableFuture<WebSocketClient> execute(WebSocketListener webSocketListener);


	/**
	 * Builds a web-socket connection that is not yet connected.
	 * It can later be connected by calling {@link WebSocketClient#connect()}
	 * @return the not yet connected web-socket
	 */
	WebSocketClient build();
}
