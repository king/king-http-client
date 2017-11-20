package com.king.platform.net.http;


import java.util.concurrent.CompletableFuture;

public interface WebSocketClient extends WebSocketConnection {
	/**
	 * Register an listener on the client
	 * @param webSocketListener the listener
	 */
	void addListener(WebSocketListener webSocketListener);

	/**
	 * Block until the server / client has closed the connection
	 * @throws InterruptedException exception if the thread waiting has been interrupted
	 */
	void awaitClose() throws InterruptedException;

	/**
	 * Connect the client to the server
	 * @return the future containing this instance.
	 */
	CompletableFuture<WebSocketClient> connect();
}
