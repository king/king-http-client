package com.king.platform.net.http;

public interface WebSocketFrameListenerAdapter extends WebSocketFrameListener {
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
	default void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

	}

	@Override
	default void onTextFrame(String payload, boolean finalFragment, int rsv) {

	}
}
