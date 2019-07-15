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
	 * Send an text frame to the server
	 * @param text the text
	 * @param finalFragment flag indicating if this frame is the final fragment
	 * @param rsv reserved bits used for protocol extensions
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendTextFrame(String text, boolean finalFragment, int rsv);

	/**
	 * Send an close frame to the server. This is to inform the server about wanting to close the connection.
	 * @return the resulting future
	 * @param statusCode the status code
	 * @param reason the reason to close
	 */
	CompletableFuture<Void> sendCloseFrame(int statusCode, String reason);


	/**
	 * Send an close frame to the server. This is to inform the server about wanting to close the connection.
	 * StatusCode 1000 (Normal close) will be used.
	 * @return the resulting future
	 *
	 */
	CompletableFuture<Void> sendCloseFrame();

	/**
	 * Send an binary frame to the server
	 * @param payload the bytes
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendBinaryFrame(byte[] payload);


	/**
	 * Send an binary frame to the server
	 * @param payload the payload
	 * @param finalFragment flag indicating if this frame is the final fragment
	 * @param rsv reserved bits used for protocol extensions
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendBinaryFrame(byte[] payload, boolean finalFragment, int rsv);


	/**
	 * Send a ping frame to the server
	 *
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendPingFrame();

	/**
	 * Send a ping frame with a payload to the server
	 *
	 * @param payload the payload
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendPingFrame(byte[] payload);

	/**
	 * Close the connection to the server (without sending any close frame)
	 * @return the resulting future
	 */
	CompletableFuture<Void> close();
}
