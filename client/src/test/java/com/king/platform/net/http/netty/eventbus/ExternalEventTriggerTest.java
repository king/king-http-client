// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import org.junit.Before;
import org.junit.Test;

import static se.mockachino.Mockachino.*;


public class ExternalEventTriggerTest {

	private ExternalEventTrigger externalEventTrigger;
	private EventListener eventListener;

	@Before
	public void setUp() throws Exception {
		externalEventTrigger = new ExternalEventTrigger();
		eventListener = mock(EventListener.class);
	}

	@Test
	public void shouldTriggerEventListenerForEvent1() throws Exception {
		externalEventTrigger.registerEventListener(eventListener);
		externalEventTrigger.trigger(Event.CLOSE, null);
		verifyOnce().on(eventListener).onEvent(Event.CLOSE, null);
	}


	@Test
	public void shouldTriggerEventListenerForEvent2() throws Exception {
		externalEventTrigger.registerEventListener(eventListener);
		externalEventTrigger.trigger(Event.ERROR, null, null);
		verifyOnce().on(eventListener).onEvent(Event.ERROR, null, null);
	}

	@Test
	public void shouldNotTriggerEventListenerForEvent2IfEventListenerIsNull() throws Exception {
		externalEventTrigger.trigger(Event.ERROR, null, null);
		verifyNever().on(eventListener).onEvent(Event.ERROR, null, null);
	}

	@Test
	public void shouldNotTriggerEventListenerForEvent1IfEventListenerIsNull() throws Exception {
		externalEventTrigger.trigger(Event.CLOSE, null);
		verifyNever().on(eventListener).onEvent(Event.CLOSE, null);
	}
}
