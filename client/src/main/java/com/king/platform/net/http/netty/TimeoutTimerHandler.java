// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

public class TimeoutTimerHandler {
	private final Timer nettyTimer;
	private final RequestEventBus requestEventBus;
	private final TimeoutTimerTask task;
	private Timeout timeout;

	public TimeoutTimerHandler(Timer nettyTimer, RequestEventBus requestEventBus, final TimeoutTimerTask task) {
		this.nettyTimer = nettyTimer;
		this.requestEventBus = requestEventBus;
		this.task = task;
	}

	public void scheduleTimeout(long delayTime, TimeUnit timeUnit) {
		timeout = nettyTimer.newTimeout(task, delayTime, timeUnit);

		requestEventBus.subscribe(Event.ERROR, this::cancel);
		requestEventBus.subscribe(Event.COMPLETED, this::complete);

	}

	private void cancel(HttpRequestContext payload1, Throwable payload2) {
		task.cancel();
		if (timeout != null) {
			timeout.cancel();
		}
	}

	private void complete(HttpRequestContext payload) {
		task.completed();
		if (timeout != null) {
			timeout.cancel();
		}
	}
}
