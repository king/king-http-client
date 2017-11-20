package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface WebSocketConnection {

	Headers headers();

	String getNegotiatedSubProtocol();

	CompletableFuture<Void> sendTextFrame(String text);

	CompletableFuture<Void> sendCloseFrame();

	CompletableFuture<Void> sendBinaryFrame(byte[] payload);
}
