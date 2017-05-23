// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface EventCallback {
	/**
	 * The callback for when the client has recived an server side event
	 *
	 * @param lastSentId last sent event id
	 * @param event      event name
	 * @param data       event data
	 */
	void onEvent(String lastSentId, String event, String data);
}
