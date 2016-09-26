// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.SseExecutionCallback;

class EmptySseExecutionCallback implements SseExecutionCallback {
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
