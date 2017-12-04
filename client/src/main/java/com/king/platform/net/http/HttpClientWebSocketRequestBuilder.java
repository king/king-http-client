package com.king.platform.net.http;

import java.time.Duration;

public interface HttpClientWebSocketRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientWebSocketRequestBuilder> {

	/**
	 * Specify what sub-protocol this client desires
	 *
	 * @param subProtocols the sub protocol
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder subProtocols(String subProtocols);

	/**
	 * Specify if the client should automatically send ping to the server to keep the connection alive.
	 *
	 * @param duration the duration between the pings
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder pingEvery(Duration duration);

	/**
	 * Specify if the client should automatically respond with pongs when it receives an ping from the server.
	 *
	 * @param autoPong true if it should respond.
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder autoPong(boolean autoPong);

	/**
	 * Specify if the client should automatically respond with the close frame to the server before closing the connection.
	 *
	 * @param autoCloseFrame true if it should respond.
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder autoCloseFrame(boolean autoCloseFrame);

	/**
	 * Build the reusable web-socket request
	 * @return the built request
	 */
	BuiltWebSocketRequest build();

}
