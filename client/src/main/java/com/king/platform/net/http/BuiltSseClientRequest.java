package com.king.platform.net.http;


public interface BuiltSseClientRequest {
	SseClient execute(SseExecutionCallback sseExecutionCallback);

	SseClient execute();
}
