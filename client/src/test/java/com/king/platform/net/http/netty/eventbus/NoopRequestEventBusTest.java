// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;


public class NoopRequestEventBusTest {

	private RequestEventBus requestEventBus;

	@Before
	public void setUp() throws Exception {
		requestEventBus = new NoopRequestEventBus();
	}

	@Test
	public void createRequestEventBusShouldReturnTheSameInstance() throws Exception {
		RequestEventBus eventBus = requestEventBus.createRequestEventBus();
		assertSame(eventBus, requestEventBus);
	}

	@Test
	public void triggeredEventShouldHaveNoEffect() throws Exception {
		final AtomicBoolean triggered = new AtomicBoolean();
		requestEventBus.subscribe(Event.CLOSE, new EventBusCallback1<Void>() {
			@Override
			public void onEvent(Void payload) {
				triggered.set(true);
			}
		});

		requestEventBus.triggerEvent(Event.CLOSE);

		assertFalse(triggered.get());
	}
}
