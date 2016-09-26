package com.king.platform.net.http;


public interface SseCallback {
	/**
	 * The callback for when the client has recived an server side event
	 * @param serverSideEvent the event
	 */
	void onEvent(ServerSideEvent serverSideEvent);
}
