package com.king.platform.net.http;


public interface HttpSSECallback {
	void onConnect();

	void onDisconnect();

	void onData(String data);

	void onComment(String comment);

	void onEvent(String name, String data);
}
