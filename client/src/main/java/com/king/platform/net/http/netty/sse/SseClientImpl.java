// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.EventCallback;
import com.king.platform.net.http.SseClient;
import com.king.platform.net.http.SseClientCallback;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.ExternalEventTrigger;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class SseClientImpl implements SseClient {
	private ExternalEventTrigger externalEventTrigger;
	private final BuiltNettyClientRequest builtNettyClientRequest;

	private final ServerEventDecoder serverEventDecoder;

	private AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);


	private AwaitLatch awaitLatch = new AwaitLatch();

	private DelegatingAsyncSseClientCallback delegatingAsyncSseClientCallback;
	private final Executor httpClientCallbackExecutor;

	public SseClientImpl(SseClientCallback callback, BuiltNettyClientRequest builtNettyClientRequest, Executor httpClientCallbackExecutor) {

		delegatingAsyncSseClientCallback = new DelegatingAsyncSseClientCallback(httpClientCallbackExecutor);
		this.httpClientCallbackExecutor = httpClientCallbackExecutor;
		if (callback != null) {
			delegatingAsyncSseClientCallback.addSseClientCallbacks(callback);
		}


		this.builtNettyClientRequest = builtNettyClientRequest;

		serverEventDecoder = new ServerEventDecoder(delegatingAsyncSseClientCallback);
		SseNioHttpCallback nioCallback = new SseNioHttpCallback(serverEventDecoder, delegatingAsyncSseClientCallback, state, builtNettyClientRequest
			.isFollowRedirects(), awaitLatch);

		builtNettyClientRequest.withNioCallback(nioCallback);
		builtNettyClientRequest.withExternalEventTrigger(() -> {
            SseClientImpl.this.externalEventTrigger = new ExternalEventTrigger();
            return externalEventTrigger;
        });
	}

	@Override
	public void onEvent(String eventName, EventCallback callback) {
		delegatingAsyncSseClientCallback.addEventCallback(eventName, callback);
	}

	@Override
	public void onEvent(EventCallback callback) {
		delegatingAsyncSseClientCallback.addEventCallback(callback);

	}

	@Override
	public void addCallback(SseClientCallback callback) {
		delegatingAsyncSseClientCallback.addSseClientCallbacks(callback);
	}

	@Override
	public void onDisconnect(DisconnectCallback disconnectCallback) {
		delegatingAsyncSseClientCallback.addCloseCallback(disconnectCallback);
	}

	@Override
	public void onConnect(ConnectCallback connectCallback) {
		delegatingAsyncSseClientCallback.addConnectCallback(connectCallback);
	}

	@Override
	public void onError(ErrorCallback errorCallback) {
		delegatingAsyncSseClientCallback.addErrorCallback(errorCallback);
	}

	@Override
	public void connect() {
		if (!state.compareAndSet(State.DISCONNECTED, State.RECONNECTING)) {
			throw new RuntimeException("sse client is not in disconnected state");
		}

		serverEventDecoder.reset();

		builtNettyClientRequest.execute();
	}

	@Override
	public void close() {
		if (externalEventTrigger != null) {
			externalEventTrigger.trigger(Event.CLOSE, null);
		}
	}

	@Override
	public void awaitClose() throws InterruptedException {
		awaitLatch.awaitClose();

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		httpClientCallbackExecutor.execute(new Runnable() { //we post a job on the executor to make sure we have flused all pending events before we return the awaitClose
			@Override
			public void run() {
				countDownLatch.countDown();
			}
		});
		countDownLatch.await();
		Thread.sleep(10);
	}

}
