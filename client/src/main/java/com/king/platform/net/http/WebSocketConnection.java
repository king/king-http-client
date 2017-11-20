package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface WebSocketConnection {

	Headers headers();

	String getNegotiatedSubProtocol();

	void addListener(WebSocketListener webSocketListener);

	CompletableFuture<WebSocketConnection> connect();

	boolean isConnected();

	void awaitClose() throws InterruptedException;

	CompletableFuture<Void> sendTextFrame(String text);

	CompletableFuture<Void> sendCloseFrame();

	CompletableFuture<Void> sendBinaryFrame(byte[] payload);
}
