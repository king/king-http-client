package com.king.platform.net.http;

public interface WebSocketClientCallback {

	void onConnect(WebSocketClient client);

	void onDisconnect(int code, String reason);

	void onError(Throwable t);

	default void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
	};

	default void onTextFrame(String payload, boolean finalFragment, int rsv) {
	};

	default void onPingFrame(byte[] payload) {
	};

	default void onPongFrame(byte[] payload) {
	};

}
