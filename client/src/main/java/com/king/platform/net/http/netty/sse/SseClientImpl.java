// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class SseClientImpl implements SseClient {
	private final ExternalEventTrigger externalEventTrigger;
	private final BuiltNettyClientRequest builtNettyClientRequest;
	private final DelegatingHttpCallback httpCallback;
	private final VoidResponseConsumer responseBodyConsumer;
	private final ServerEventDecoder serverEventDecoder;
	private final DelegatingNioHttpCallback nioCallback;

	private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);

	private final ConcurrentHashMap<String, List<SseCallback>> eventCallbackMap = new ConcurrentHashMap<>();
	private List<SseCallback> dataCallback = new CopyOnWriteArrayList<>();

	private CountDownLatch countDownLatch;

	public SseClientImpl(SseExecutionCallback providedSseExecutionCallback, BuiltNettyClientRequest builtNettyClientRequest, Executor httpClientCallbackExecutor) {
		if (providedSseExecutionCallback == null) {
			providedSseExecutionCallback = new EmptySseExecutionCallback();
		}

		providedSseExecutionCallback = new WrappedSseExecutionCallback(providedSseExecutionCallback);

		this.builtNettyClientRequest = builtNettyClientRequest;

		externalEventTrigger = new ExternalEventTrigger();

		httpCallback = new DelegatingHttpCallback(providedSseExecutionCallback);
		responseBodyConsumer = new VoidResponseConsumer();
		serverEventDecoder = new ServerEventDecoder(providedSseExecutionCallback, httpClientCallbackExecutor);
		nioCallback = new DelegatingNioHttpCallback(serverEventDecoder, providedSseExecutionCallback, httpClientCallbackExecutor);
	}


	@Override
	public void close() {
		externalEventTrigger.trigger(Event.CLOSE, null);
	}

	@Override
	public void subscribe(String eventName, SseCallback callback) {
		List<SseCallback> sseCallbacks = eventCallbackMap.get(eventName);
		if (sseCallbacks == null) {
			sseCallbacks = new CopyOnWriteArrayList<>();
			List<SseCallback> prevValue = eventCallbackMap.putIfAbsent(eventName, sseCallbacks);
			if (prevValue != null) {
				sseCallbacks = prevValue;
			}
		}
		sseCallbacks.add(callback);
	}

	@Override
	public void subscribe(SseCallback callback) {
		dataCallback.add(callback);
	}

	@Override
	public void awaitClose() throws  InterruptedException {
		countDownLatch.await();
	}


	@Override
	public void connect() {
		if (!state.compareAndSet(State.DISCONNECTED, State.RECONNECTING)) {
			throw new RuntimeException("sse client is not in disconnected state");
		}

		if (countDownLatch != null) {
			countDownLatch.countDown();
		}

		countDownLatch = new CountDownLatch(1);

		serverEventDecoder.reset();

		builtNettyClientRequest.execute(httpCallback, responseBodyConsumer, nioCallback, externalEventTrigger);
	}

	private class WrappedSseExecutionCallback implements SseExecutionCallback {
		private final SseExecutionCallback sseExecutionCallback;

		private WrappedSseExecutionCallback(SseExecutionCallback sseExecutionCallback) {
			this.sseExecutionCallback = sseExecutionCallback;
		}

		@Override
		public void onConnect() {
			sseExecutionCallback.onConnect();
		}

		@Override
		public void onDisconnect() {
			sseExecutionCallback.onDisconnect();
		}

		@Override
		public void onError(Throwable throwable) {
			sseExecutionCallback.onError(throwable);
		}

		@Override
		public void onEvent(String lastSentId, String event, String data) {
			sseExecutionCallback.onEvent(lastSentId, event, data);

			invokeCallbacks(lastSentId, event, data, dataCallback);

			if (event != null) {
				List<SseCallback> sseCallbacks = eventCallbackMap.get(event);
				invokeCallbacks(lastSentId, event, data, sseCallbacks);
			}
		}

		private void invokeCallbacks(String lastSentId, String event, String data, List<SseCallback> sseCallbacks) {
			if (sseCallbacks != null) {
				for (SseCallback sseCallback : sseCallbacks) {
					sseCallback.onEvent(lastSentId, event, data);
				}
			}
		}

	}

	private class DelegatingNioHttpCallback implements NioCallback {

		private final ServerEventDecoder serverEventDecoder;
		private final SseExecutionCallback providedSseExecutionCallback;
		private final Executor httpClientCallbackExecutor;

		public DelegatingNioHttpCallback(ServerEventDecoder serverEventDecoder, SseExecutionCallback providedSseExecutionCallback, Executor httpClientCallbackExecutor) {
			this.serverEventDecoder = serverEventDecoder;
			this.providedSseExecutionCallback = providedSseExecutionCallback;
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
					providedSseExecutionCallback.onConnect();
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
		private final SseExecutionCallback sseExecutionCallback;

		DelegatingHttpCallback(SseExecutionCallback sseExecutionCallback) {
			this.sseExecutionCallback = sseExecutionCallback;
		}

		@Override
		public void onCompleted(HttpResponse<Void> httpResponse) {
			sseExecutionCallback.onDisconnect();
			state.set(State.DISCONNECTED);
			countDownLatch.countDown();

		}

		@Override
		public void onError(Throwable throwable) {
			sseExecutionCallback.onError(throwable);
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
