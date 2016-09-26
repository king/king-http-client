package com.king.platform.net.http;


import java.util.concurrent.ExecutionException;

public interface SseClient {
	void close();

	void subscribe(String evenName, SseCallback callback);

	void subscribe(SseCallback callback);

	void awaitClose() throws ExecutionException, InterruptedException;

	void reconnect();
}
