package com.king.platform.net.http;

public interface WebSocketMessageListener extends WebSocketConnectionListener {
	/**
	 * Callback for when the server has sent an binary frame to the client.
	 *
	 * @param message the aggregated payload
	 */
	void onBinaryMessage(byte[] message);

	/**
	 * Callback for when the server has sent an text frame to the client.
	 *
	 * @param message the aggregated payload
	 */
	void onTextMessage(String message);

}
