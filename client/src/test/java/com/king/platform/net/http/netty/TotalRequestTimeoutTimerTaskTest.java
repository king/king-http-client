// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import io.netty.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class TotalRequestTimeoutTimerTaskTest {
	private TotalRequestTimeoutTimerTask timeoutTimerTask;
	private RequestEventBus requestEventBus;
	private HttpRequestContext httpRequestContext;

	@BeforeEach
	public void setUp() throws Exception {
		requestEventBus = mock(RequestEventBus.class);
		httpRequestContext = mock(HttpRequestContext.class);
		timeoutTimerTask = new TotalRequestTimeoutTimerTask(requestEventBus, httpRequestContext);
	}

	@Test
	public void shouldTriggerTheFirstTime() throws Exception {
		timeoutTimerTask.run(mock(Timeout.class));
		verify(requestEventBus).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(Throwable.class));
	}

	@Test
	public void shouldNotTriggerTwice() throws Exception {
		timeoutTimerTask.run(mock(Timeout.class));
		timeoutTimerTask.run(mock(Timeout.class));
		verify(requestEventBus).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(Throwable.class));
	}

	@Test
	public void shouldNotTriggerIfCompleted() throws Exception {
		timeoutTimerTask.completed();
		timeoutTimerTask.run(mock(Timeout.class));
		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(Throwable.class));
	}

	@Test
	public void shouldNotTriggerIfCanceled() throws Exception {
		timeoutTimerTask.cancel();
		timeoutTimerTask.run(mock(Timeout.class));
		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(Throwable.class));
	}

	@Test
	public void shouldNotTriggerIfCanceledAndCompleted() throws Exception {
		timeoutTimerTask.cancel();
		timeoutTimerTask.completed();
		timeoutTimerTask.run(mock(Timeout.class));
		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(Throwable.class));
	}
}
