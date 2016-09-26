package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.request.ServerEventDecoder;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class SseClientImpl implements SseClient {
	private final ExternalEventTrigger externalEventTrigger;
	private final BuiltNettyClientRequest builtNettyClientRequest;
	private final DelegatingHttpCallback httpCallback;
	private final VoidResponseConsumer responseBodyConsumer;
	private final ServerEventDecoder serverEventDecoder;
	private final DelegatingNioHttpCallback nioCallback;

	private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);

	private final ConcurrentHashMap<String, SseCallback> eventCallbackMap = new ConcurrentHashMap<>();
	private SseCallback dataCallback;

	private CountDownLatch countDownLatch;

	public SseClientImpl(HttpSseCallback providedHttpSseCallback, BuiltNettyClientRequest builtNettyClientRequest, Executor httpClientCallbackExecutor) {
		if (providedHttpSseCallback == null) {
			providedHttpSseCallback = new EmptyHttpSseCallback();
		}

		providedHttpSseCallback = new WrappedHttpSseCallback(providedHttpSseCallback);

		this.builtNettyClientRequest = builtNettyClientRequest;

		externalEventTrigger = new ExternalEventTrigger();

		httpCallback = new DelegatingHttpCallback(providedHttpSseCallback);
		responseBodyConsumer = new VoidResponseConsumer();
		serverEventDecoder = new ServerEventDecoder(providedHttpSseCallback, httpClientCallbackExecutor);
		nioCallback = new DelegatingNioHttpCallback(serverEventDecoder, providedHttpSseCallback, httpClientCallbackExecutor);
	}


	@Override
	public void close() {
		externalEventTrigger.trigger(Event.CLOSE, null);
	}

	@Override
	public void subscribe(String eventName, SseCallback callback) {
		eventCallbackMap.put(eventName, callback);
	}

	@Override
	public void subscribe(SseCallback callback) {
		dataCallback = callback;
	}

	@Override
	public void awaitClose() throws  InterruptedException {
		countDownLatch.await();
	}

	public void connect() {
		reconnect();
	}

	@Override
	public void reconnect() {
		if (!state.compareAndSet(State.DISCONNECTED, State.RECONNECTING)) {
			throw new RuntimeException("sse client is not in disconnected state");
		}

		countDownLatch = new CountDownLatch(1);

		serverEventDecoder.reset();

		builtNettyClientRequest.execute(httpCallback, responseBodyConsumer, nioCallback, externalEventTrigger);
	}

	private class WrappedHttpSseCallback implements HttpSseCallback {
		private final HttpSseCallback httpSseCallback;

		private WrappedHttpSseCallback(HttpSseCallback httpSseCallback) {
			this.httpSseCallback = httpSseCallback;
		}

		@Override
		public void onConnect() {
			httpSseCallback.onConnect();
		}

		@Override
		public void onDisconnect() {
			httpSseCallback.onDisconnect();
		}

		@Override
		public void onError(Throwable throwable) {
			httpSseCallback.onError(throwable);
		}

		@Override
		public void onEvent(String lastSentId, String event, String data) {
			httpSseCallback.onEvent(lastSentId, event, data);
			if (dataCallback != null) {
				dataCallback.onEvent(new ServerSideEventImpl(lastSentId, event, data));
			}
			if (event != null) {
				SseCallback sseCallback = eventCallbackMap.get(event);
				if (sseCallback != null) {
					sseCallback.onEvent(new ServerSideEventImpl(lastSentId, event, data));
				}
			}
		}

	}

	private class DelegatingNioHttpCallback implements NioCallback {

		private final ServerEventDecoder serverEventDecoder;
		private final HttpSseCallback providedHttpSseCallback;
		private final Executor httpClientCallbackExecutor;

		public DelegatingNioHttpCallback(ServerEventDecoder serverEventDecoder, HttpSseCallback providedHttpSseCallback, Executor httpClientCallbackExecutor) {
			this.serverEventDecoder = serverEventDecoder;
			this.providedHttpSseCallback = providedHttpSseCallback;
			this.httpClientCallbackExecutor = httpClientCallbackExecutor;
		}

		@Override
		public void onConnecting() {

		}

		@Override
		public void onConnected() {
			state.set(State.CONNECTED);
			httpClientCallbackExecutor.execute(new Runnable() {
				@Override
				public void run() {
					providedHttpSseCallback.onConnect();
				}
			});
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
			countDownLatch.countDown();

		}

		@Override
		public void onError(Throwable throwable) {
			httpSseCallback.onError(throwable);
			state.set(State.DISCONNECTED);
			countDownLatch.countDown();
		}
	}


	private enum State {
		CONNECTED,
		DISCONNECTED,
		RECONNECTING,
	}

}
