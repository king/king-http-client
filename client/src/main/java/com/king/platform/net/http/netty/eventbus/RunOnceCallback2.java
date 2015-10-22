// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RunOnceCallback2<T1, T2> implements EventBusCallback2<T1, T2> {
	private AtomicBoolean executed = new AtomicBoolean();

	public abstract void onFirstEvent(Event2 event, T1 payload1, T2 payload2);

	@Override
	public void onEvent(Event2<T1, T2> event, T1 payload1, T2 payload2) {
		if (executed.compareAndSet(false, true)) {
			onFirstEvent(event, payload1, payload2);
		}
	}
}
