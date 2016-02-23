package com.king.platform.net.http;


public interface HttpSSECallback {
	void onConnect();

	void onDisconnect();

	void onError(Throwable throwable);

	void onData(String data);

	void onEvent(String name, String data);
}
