package com.king.platform.net.http;


public interface SseCallback {
	/**
	 * The callback for when the client has recived an server side event
	 * @param lastSentId
	 * @param event
	 * @param data
	 */
	void onEvent(String lastSentId, String event, String data);
}
