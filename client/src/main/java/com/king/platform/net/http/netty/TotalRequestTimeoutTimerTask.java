// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import io.netty.util.Timeout;

import java.util.concurrent.atomic.AtomicBoolean;

public class TotalRequestTimeoutTimerTask implements TimeoutTimerTask {
	private final AtomicBoolean canceled = new AtomicBoolean();
	private final AtomicBoolean completed = new AtomicBoolean();
	private final AtomicBoolean executed = new AtomicBoolean();
	private final RequestEventBus requestEventBus;
	private final HttpRequestContext httpRequestContext;

	public TotalRequestTimeoutTimerTask(RequestEventBus requestEventBus, HttpRequestContext httpRequestContext) {
		this.requestEventBus = requestEventBus;
		this.httpRequestContext = httpRequestContext;
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if (executed.compareAndSet(false, true) && !canceled.get() && !completed.get()) {
			requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, new TimeoutException("Request timed out"));
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


}
