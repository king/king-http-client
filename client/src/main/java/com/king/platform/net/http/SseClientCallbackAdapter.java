package com.king.platform.net.http;


public interface SseClientCallbackAdapter extends SseClientCallback {
	@Override
	default void onConnect() {

	}

	@Override
	default void onDisconnect() {

	}

	@Override
	default void onError(Throwable throwable) {

	}

	@Override
	default void onEvent(String lastSentId, String event, String data) {

	}
}
