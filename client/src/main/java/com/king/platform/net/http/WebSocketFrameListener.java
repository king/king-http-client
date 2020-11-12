package com.king.platform.net.http;

public interface WebSocketFrameListener extends WebSocketConnectionListener{


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
	void onTextFrame(byte[] payload, boolean finalFragment, int rsv);


}
