package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface BuiltWebSocketRequest {
	CompletableFuture<WebSocketConnection> execute(WebSocketListener webSocketListener);
}
