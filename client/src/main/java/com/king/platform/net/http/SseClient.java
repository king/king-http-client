// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface SseClient {
	/**
	 * Subscribe to an specific event
	 * @param eventName the event name
	 * @param callback the callback object
	 */
	void onEvent(String eventName, EventCallback callback);

	/**
	 * Subscribe to all events
	 * @param callback the callback object
	 */
	void onEvent(EventCallback callback);


	/**
	 * Register a sse client callback
	 * @param callback the callback object
	 */
	void addCallback(SseClientCallback callback);


	/**
	 * Register a disconnect callback
	 * @param disconnectCallback the callback object
	 */
	void onDisconnect(DisconnectCallback disconnectCallback);

	/**
	 * Register a connect callback
	 * @param connectCallback the callback object
	 */
	void onConnect(ConnectCallback connectCallback);


	/**
	 * Connect the client to the server. Can be used to either establish the initial connection, or reconnect a failed connection.
	 */
	void connect();

	/**
	 * Close the current connection to the server
	 */
	void close();

	/**
	 * Block until the server / client has closed the connection
	 * @throws InterruptedException exception if the thread waiting has been interrupted
	 */
	void awaitClose() throws InterruptedException;


	interface DisconnectCallback {
		void onDisconnect();
	}

	interface ConnectCallback {
		void onConnect();
	}

}
