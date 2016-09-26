package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.request.ServerEventDecoder;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SseClientImpl implements SseClient {
	private final ExternalEventTrigger externalEventTrigger;
	private final BuiltNettyClientRequest builtNettyClientRequest;
	private final DelegatingHttpCallback httpCallback;
	private final VoidResponseConsumer responseBodyConsumer;
	private final ServerEventDecoder serverEventDecoder;

	private Future<FutureResult<Void>> future;

	private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);

	public SseClientImpl(HttpSseCallback providedHttpSseCallback, BuiltNettyClientRequest builtNettyClientRequest, Executor httpClientCallbackExecutor) {
		this.builtNettyClientRequest = builtNettyClientRequest;

		externalEventTrigger = new ExternalEventTrigger();

		httpCallback = new DelegatingHttpCallback(providedHttpSseCallback);
		responseBodyConsumer = new VoidResponseConsumer();
		serverEventDecoder = new ServerEventDecoder(providedHttpSseCallback, httpClientCallbackExecutor);
	}


	@Override
	public void close() {
		externalEventTrigger.trigger(Event.CLOSE, null);
	}

	@Override
	public void subscribe(String evenName, SseCallback callback) {

	}

	@Override
	public void subscribe(SseCallback callback) {

	}

	@Override
	public void awaitClose() throws ExecutionException, InterruptedException {
		future.get();
	}

	public void connect() {
		reconnect();
	}

	@Override
	public void reconnect() {
		if (!state.compareAndSet(State.DISCONNECTED, State.RECONNECTING)) {
			throw new RuntimeException("sse client is not in disconnected state");
		}


		DelegatingNioHttpCallback nioCallback = new DelegatingNioHttpCallback(serverEventDecoder);

		future = builtNettyClientRequest.execute(httpCallback, responseBodyConsumer, nioCallback, externalEventTrigger);
	}

	private class DelegatingNioHttpCallback implements NioCallback {

		private final ServerEventDecoder serverEventDecoder;

		public DelegatingNioHttpCallback(ServerEventDecoder serverEventDecoder) {
			this.serverEventDecoder = serverEventDecoder;
		}

		@Override
		public void onConnecting() {

		}

		@Override
		public void onConnected() {
			state.set(State.CONNECTED);
		}

		@Override
		public void onWroteHeaders() {

		}

		@Override
		public void onWroteContentProgressed(long progress, long total) {

		}

		@Override
		public void onWroteContentCompleted() {

		}

		@Override
		public void onReceivedStatus(HttpResponseStatus httpResponseStatus) {

		}

		@Override
		public void onReceivedHeaders(HttpHeaders httpHeaders) {

		}

		@Override
		public void onReceivedContentPart(int len, ByteBuf buffer) {
			serverEventDecoder.onReceivedContentPart(buffer);
		}

		@Override
		public void onReceivedCompleted(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {

		}

		@Override
		public void onError(Throwable throwable) {

		}
	}


	private class DelegatingHttpCallback implements HttpCallback<Void> {
		private final HttpSseCallback httpSseCallback;

		DelegatingHttpCallback(HttpSseCallback httpSseCallback) {
			this.httpSseCallback = httpSseCallback;
		}

		@Override
		public void onCompleted(HttpResponse<Void> httpResponse) {
			httpSseCallback.onDisconnect();
			state.set(State.DISCONNECTED);

		}

		@Override
		public void onError(Throwable throwable) {
			httpSseCallback.onError(throwable);
			state.set(State.DISCONNECTED);
		}
	}


	private static class VoidResponseConsumer implements ResponseBodyConsumer<Void> {
		@Override
		public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
		}

		@Override
		public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		}

		@Override
		public void onCompletedBody() throws Exception {
		}

		@Override
		public Void getBody() {
			return null;
		}
	}


	private enum State {
		CONNECTED,
		DISCONNECTED,
		RECONNECTING,
	}
}
