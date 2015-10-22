// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

public class TimeoutTimerHandler {
	private final Timer nettyTimer;
	private final RequestEventBus requestEventBus;

	public TimeoutTimerHandler(Timer nettyTimer, RequestEventBus requestEventBus) {
		this.nettyTimer = nettyTimer;
		this.requestEventBus = requestEventBus;
	}

	public void scheduleTimeout(final TimeoutTimerTask task, long delayTime, TimeUnit timeUnit) {
		final Timeout timeout = nettyTimer.newTimeout(task, delayTime, timeUnit);

		requestEventBus.subscribe(Event.ERROR, new EventBusCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onEvent(Event2<HttpRequestContext, Throwable> event, HttpRequestContext payload1, Throwable payload2) {
				task.cancel();
				timeout.cancel();
			}
		});

		requestEventBus.subscribe(Event.COMPLETED, new EventBusCallback1<HttpRequestContext>() {
			@Override
			public void onEvent(Event1<HttpRequestContext> event, HttpRequestContext payload) {
				task.completed();
				timeout.cancel();
			}
		});

	}
}
