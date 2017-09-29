package com.king.platform.net.http;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public interface NioCallbackAdapter extends NioCallback {
	@Override
	default void onConnecting() {

	}

	@Override
	default void onConnected() {

	}

	@Override
	default void onWroteHeaders() {

	}

	@Override
	default void onWroteContentProgressed(long progress, long total) {

	}

	@Override
	default void onWroteContentCompleted() {

	}

	@Override
	default void onReceivedStatus(HttpResponseStatus httpResponseStatus) {

	}

	@Override
	default void onReceivedHeaders(HttpHeaders httpHeaders) {

	}

	@Override
	default void onReceivedContentPart(int len, ByteBuf buffer) {

	}

	@Override
	default void onReceivedCompleted(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {

	}

	@Override
	default void onError(Throwable throwable) {

	}
}
