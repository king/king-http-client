package com.king.platform.net.http;


public interface BuiltSSEClientRequest {
	SseClient execute(HttpSseCallback httpSseCallback);

	SseClient execute();
}
