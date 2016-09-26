package com.king.platform.net.http;


public interface HttpSseCallback {
	void onConnect();

	void onDisconnect();

	void onError(Throwable throwable);

	void onEvent(String lastSentId, String event, String data);
}
