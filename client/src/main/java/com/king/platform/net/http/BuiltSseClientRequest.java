// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

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
