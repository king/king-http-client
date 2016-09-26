package com.king.platform.net.http;


public interface SseClient {
	/**
	 * Close the current connection to the server
	 */
	void close();

	/**
	 * Subscribe to an specific event
	 * @param eventName the event name
	 * @param callback the callback object
	 */
	void subscribe(String eventName, SseCallback callback);

	/**
	 * Subscribe to all events
	 * @param callback the callback object
	 */
	void subscribe(SseCallback callback);

	/**
	 * Block until the server / client has closed the connection
	 * @throws InterruptedException
	 */
	void awaitClose() throws InterruptedException;

	/**
	 * Connect the client to the server. Can be used to either establish the initial connection, or reconnect a failed connection.
	 */
	void connect();
}
