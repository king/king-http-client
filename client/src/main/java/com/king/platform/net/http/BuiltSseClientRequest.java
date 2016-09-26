package com.king.platform.net.http;


public interface BuiltSseClientRequest {
	/**
	 * Build the SseClient and execute it against the server
	 * @param sseExecutionCallback the callback object
	 * @return the built SseClient
	 */
	SseClient execute(SseExecutionCallback sseExecutionCallback);

	/**
	 * Build the SseClient and execute it against the server
	 * @return the built SseClient
	 */
	SseClient execute();


	SseClient build(SseExecutionCallback sseExecutionCallback);

	SseClient build();

}
