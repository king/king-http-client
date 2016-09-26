package com.king.platform.net.http;


public interface SseClient {
	void close();

	void subscribe(String eventName, SseCallback callback);

	void subscribe(SseCallback callback);

	void awaitClose() throws InterruptedException;

	void reconnect();
}
