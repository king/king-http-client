package com.king.platform.net.http;

import java.util.concurrent.CompletableFuture;

public interface WebSocketConnection {

	/**
	 * Return the current headers for this connection
	 * @return headers
	 */
	Headers headers();

	/**
	 * The current negotiated sub protocols the server has decided on.
	 * @return the protocol
	 */
	String getNegotiatedSubProtocol();

	/**
	 * Is the current web socket connected to the server
	 * @return true if connected
	 */
	boolean isConnected();

	/**
	 * Send an text frame to the server
	 * @param text the text
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendTextFrame(String text);

	/**
	 * Send an close frame to the server. This is to inform the server about wanting to close the connection.
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendCloseFrame();

	/**
	 * Send an binary frame to the server
	 * @param payload the bytes
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendBinaryFrame(byte[] payload);
}
