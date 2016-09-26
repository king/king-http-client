// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface SseExecutionCallback {
	/**
	 * Callback for when the client has connected to the server
	 */
	void onConnect();

	/**
	 * Callback for when the client has disconnected from the server
	 */
	void onDisconnect();


	/**
	 * Callback for when the client has thrown an error
	 *
	 * @param throwable the error
	 */
	void onError(Throwable throwable);


	/**
	 * Callback for when the client has received an event from the server
	 *
	 * @param lastSentId the last sent event id
	 * @param event      the event type/name
	 * @param data       the data of the event
	 */
	void onEvent(String lastSentId, String event, String data);
}
