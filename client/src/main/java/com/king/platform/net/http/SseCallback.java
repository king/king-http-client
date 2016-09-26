package com.king.platform.net.http;


public interface SseCallback {
	/**
	 * The callback for when the client has recived an server side event
	 *
	 * @param lastSentId last sent event id
	 * @param event      event name
	 * @param data       event data
	 */
	void onEvent(String lastSentId, String event, String data);
}
