// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RunOnceCallback1<T> implements EventBusCallback1<T> {
	private final AtomicBoolean executed = new AtomicBoolean();

	public abstract void onFirstEvent(T payload);

	@Override
	public void onEvent(T payload) {
		if (executed.compareAndSet(false, true)) {
			onFirstEvent(payload);
		}
	}
}
