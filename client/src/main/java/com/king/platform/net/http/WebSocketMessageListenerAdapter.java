package com.king.platform.net.http;

public interface WebSocketMessageListenerAdapter extends WebSocketMessageListener {
	@Override
	default void onBinaryMessage(byte[] message) {
	}

	@Override
	default void onTextMessage(String message) {
	}

	@Override
	default void onConnect(WebSocketConnection connection) {
	}

	@Override
	default void onError(Throwable throwable) {
	}

	@Override
	default void onDisconnect() {
	}

	@Override
	default void onCloseFrame(int code, String reason) {
	}

	@Override
	default void onPingFrame(byte[] payload) {

	}

	@Override
	default void onPongFrame(byte[] payload) {

	}
}
