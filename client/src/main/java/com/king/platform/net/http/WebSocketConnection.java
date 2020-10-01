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
	 * Send an text frame or message to the server
	 * @param text the text
	 * @return the resulting future
	 * @deprecated Use either {@link #sendTextMessage(String)} to send a complete message that might get fragmented into multiple frames, or use
	 * {@link #sendTextFrame(String, boolean, int)} to send an frame.
	 */
	@Deprecated
	CompletableFuture<Void> sendTextFrame(String text);

	/**
	 * Send a complete text message to the server. The message might get split up into multiple frames if it is longer then {@link HttpClientWebSocketRequestBuilder#maxOutgoingFrameSize(int)}
	 * @param text the text message
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendTextMessage(String text);

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
	 * Send an binary frame or message to the server
	 * @param payload the bytes
	 * @return the resulting future
	 * @deprecated Use either {@link #sendBinaryMessage(byte[])} to send a complete message that might get fragmented into multiple frames, or use
	 * {@link #sendBinaryFrame(byte[], boolean, int)} to send an frame.
	 */
	@Deprecated
	CompletableFuture<Void> sendBinaryFrame(byte[] payload);

	/**
	 * Send a complete binary message to the server. The message might get split up into multiple frames if it is longer then {@link HttpClientWebSocketRequestBuilder#maxOutgoingFrameSize(int)}
	 * @param payload the binary payload
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendBinaryMessage(byte[] payload);


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
	 * Send a pong frame to the server
	 *
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendPongFrame();

	/**
	 * Send a pong frame with a payload to the server
	 *
	 * @param payload the payload
	 * @return the resulting future
	 */
	CompletableFuture<Void> sendPongFrame(byte[] payload);

	/**
	 * Close the connection to the server (without sending any close frame)
	 * @return the resulting future
	 */
	CompletableFuture<Void> close();
}
