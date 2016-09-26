package com.king.platform.net.http;


public interface BuiltSSEClientRequest {
	SseClient execute(SseExecutionCallback sseExecutionCallback);

	SseClient execute();
}
