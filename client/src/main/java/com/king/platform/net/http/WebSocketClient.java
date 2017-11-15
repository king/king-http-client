package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface WebSocketClient {

	Headers getHeaders();

	CompletableFuture<Void> sendTextFrame(String text);

	CompletableFuture<Void> sendCloseFrame();

	CompletableFuture<Void> sendBinaryFrame(byte[] payload);
}
