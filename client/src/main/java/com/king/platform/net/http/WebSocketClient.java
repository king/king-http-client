package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface WebSocketClient {

	CompletableFuture<Void> sendTextFrame(String text);


}
