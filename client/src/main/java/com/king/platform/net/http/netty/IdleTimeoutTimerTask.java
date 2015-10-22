// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.EventBusCallback1;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdleTimeoutTimerTask implements TimeoutTimerTask {
	private final AtomicBoolean canceled = new AtomicBoolean();
	private final AtomicBoolean completed = new AtomicBoolean();
	private final AtomicBoolean executing = new AtomicBoolean();


	private final HttpRequestContext httpRequestContext;
	private final TimeoutTimerHandler timeoutTimerHandler;
	private final long maxIdleTime;
	private final TimeProvider timeProvider;
	private final RequestEventBus requestEventBus;
	private final long requestTimeoutInstant;
	private volatile long lastTouched;

	public IdleTimeoutTimerTask(HttpRequestContext httpRequestContext, TimeoutTimerHandler timeoutTimerHandler, long maxIdleTime, int
		totalRequestTimeoutMillis, TimeProvider timeProvider, RequestEventBus requestEventBus) {
		this.httpRequestContext = httpRequestContext;
		this.timeoutTimerHandler = timeoutTimerHandler;
		this.maxIdleTime = maxIdleTime;
		this.timeProvider = timeProvider;
		this.requestEventBus = requestEventBus;
		touch();

		requestTimeoutInstant = totalRequestTimeoutMillis >= 0 ? timeProvider.currentTimeInMillis() + totalRequestTimeoutMillis : Long.MAX_VALUE;

		requestEventBus.subscribe(Event.TOUCH, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Event1 event, Void payload) {
				touch();
			}
		});
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if (completed.get() || canceled.get()) {
			return;
		}

		if (executing.compareAndSet(false, true)) {

			long now = timeProvider.currentTimeInMillis();

			long currentReadTimeoutInstant = maxIdleTime + lastTouched;
			long durationBeforeCurrentReadTimeout = currentReadTimeoutInstant - now;

			if (durationBeforeCurrentReadTimeout <= 0L) {
				// idleConnectTimeout reached
				long durationSinceLastTouch = now - lastTouched;

				String message = "Idle timeout of " + maxIdleTime + " ms was " + durationSinceLastTouch + " ms since last event";

				requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new TimeoutException(message));

				completed();
			} else if (currentReadTimeoutInstant < requestTimeoutInstant) {
				timeoutTimerHandler.scheduleTimeout(this, durationBeforeCurrentReadTimeout, TimeUnit.MILLISECONDS);
				executing.set(false);
			} else {

				cancel();  //since totalRequestTimeoutMillis will happen before next idle timeout, we dont do any thing
			}

		}

	}

	@Override
	public void completed() {
		completed.set(true);

	}

	@Override
	public void cancel() {
		canceled.set(true);
	}


	private void touch() {
		lastTouched = timeProvider.currentTimeInMillis();
	}
}
