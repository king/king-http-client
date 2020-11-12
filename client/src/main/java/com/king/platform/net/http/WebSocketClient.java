package com.king.platform.net.http;


import java.util.concurrent.CompletableFuture;

public interface WebSocketClient extends WebSocketConnection {
	/**
	 * Register an listener on the client
	 *
	 * @param webSocketListener the listener
	 * @deprecated Use {@link #addListener(WebSocketFrameListener)} or {@link #addListener(WebSocketMessageListener)} instead.
	 */
	@Deprecated
	void addListener(WebSocketListener webSocketListener);


	/**
	 * Registers an listener that receives raw websocket frames without any aggregation
	 * @param frameListener the listener
	 */
	void addListener(WebSocketFrameListener frameListener);

	/**
	 * Register a listener that receives aggregated websocket frames
	 * @param messageListener the listener
	 */
	void addListener(WebSocketMessageListener messageListener);


	/**
	 * Block until the server / client has closed the connection
	 *
	 * @throws InterruptedException exception if the thread waiting has been interrupted
	 */
	void awaitClose() throws InterruptedException;

	/**
	 * Connect the client to the server
	 *
	 * @return the future containing this instance.
	 */
	CompletableFuture<WebSocketClient> connect();


}
