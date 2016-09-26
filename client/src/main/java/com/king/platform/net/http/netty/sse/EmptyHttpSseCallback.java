package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.HttpSseCallback;

class EmptyHttpSseCallback implements HttpSseCallback {
	@Override
	public void onConnect() {

	}

	@Override
	public void onDisconnect() {

	}

	@Override
	public void onError(Throwable throwable) {

	}

	@Override
	public void onEvent(String lastSentId, String event, String data) {

	}
}
