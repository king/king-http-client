package com.king.platform.net.http;


public interface HttpSSECallback {
	void onConnect();

	void onDisconnect();

	void onError(Throwable throwable);

	void onEvent(String lastSentId, String event, String data);

	class ServerSideEvent {
		private String id;
		private String event;
		private String data;
	}
}
