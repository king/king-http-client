package com.king.platform.net.http;

public interface WebSocketListener {
	/**
	 * Callback for when the web-socket is connected
	 *
	 * @param connection the current connection to the web-socket
	 */
	void onConnect(WebSocketConnection connection);

	/**
	 * Callback for when there is some exception with the web-socket client
	 *
	 * @param throwable the exception
	 */
	void onError(Throwable throwable);

	/**
	 * Callback for when the web-socket has disconnected from the server.
	 */
	void onDisconnect();

	/**
	 * Callback for when the client has received a close frame from the server.
	 * There is no need to echo close frames back to the server.
	 *
	 * @param code   the close code
	 * @param reason the close reason
	 */
	void onCloseFrame(int code, String reason);

	/**
	 * Callback for when the server has sent an binary frame to the client.
	 *
	 * @param payload       the payload
	 * @param finalFragment Flag to indicate if this frame is the final fragment in a message. The first fragment (frame) may also be the final fragment.
	 * @param rsv           Bits used for extensions to the standard.
	 */
	void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv);

	/**
	 * Callback for when the server has sent an text frame to the client.
	 *
	 * @param payload       the payload
	 * @param finalFragment Flag to indicate if this frame is the final fragment in a message. The first fragment (frame) may also be the final fragment.
	 * @param rsv           Bits used for extensions to the standard.
	 */
	void onTextFrame(String payload, boolean finalFragment, int rsv);

}
