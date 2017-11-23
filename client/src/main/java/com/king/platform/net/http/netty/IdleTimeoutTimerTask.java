// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.Event;
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

	private final long maxIdleTime;
	private final TimeProvider timeProvider;
	private final RequestEventBus requestEventBus;
	private final long requestTimeoutInstant;
	private volatile long lastTouched;

	private TimeoutTimerHandler timeoutTimerHandler;

	public IdleTimeoutTimerTask(HttpRequestContext httpRequestContext, long maxIdleTime, int
		totalRequestTimeoutMillis, TimeProvider timeProvider, RequestEventBus requestEventBus) {
		this.httpRequestContext = httpRequestContext;
		this.maxIdleTime = maxIdleTime;
		this.timeProvider = timeProvider;
		this.requestEventBus = requestEventBus;
		touch(null);

		requestTimeoutInstant = totalRequestTimeoutMillis >= 0 ? timeProvider.currentTimeInMillis() + totalRequestTimeoutMillis : Long.MAX_VALUE;

		requestEventBus.subscribe(Event.TOUCH, this::touch);
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
			} else
				timeoutTimerHandler.scheduleTimeout(durationBeforeCurrentReadTimeout, TimeUnit.MILLISECONDS);
				executing.set(false);
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


	private void touch(Void v) {
		lastTouched = timeProvider.currentTimeInMillis();
	}

	public void setTimeoutTimerHandler(TimeoutTimerHandler timeoutTimerHandler) {
		this.timeoutTimerHandler = timeoutTimerHandler;

	}

}
