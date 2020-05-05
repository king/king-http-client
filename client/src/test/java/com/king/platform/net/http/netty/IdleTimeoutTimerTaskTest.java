// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.util.TimeProviderForTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


public class IdleTimeoutTimerTaskTest {

	private TimeProviderForTesting timeProvider;
	private IdleTimeoutTimerTask idleTimeoutTimerTask;
	private TimeoutTimerHandler timeoutTimerHandler;
	private HttpRequestContext httpRequestContext;
	private RequestEventBus requestEventBus;

	@BeforeEach
	public void setUp() throws Exception {
		timeoutTimerHandler = mock(TimeoutTimerHandler.class);
		httpRequestContext = mock(HttpRequestContext.class);
		when(httpRequestContext.getServerInfo()).thenReturn(ServerInfo.buildFromUri("http://localhost:8080"));

		timeProvider = new TimeProviderForTesting();

		requestEventBus = spy(new DefaultEventBus());
		idleTimeoutTimerTask = new IdleTimeoutTimerTask(httpRequestContext, 100, 2000, timeProvider, requestEventBus);
		idleTimeoutTimerTask.setTimeoutTimerHandler(timeoutTimerHandler);
	}

	@Test
	public void shouldTimeoutAfterIdle() throws Exception {

		timeProvider.forwardMillis(200);

		idleTimeoutTimerTask.run(null);

		verify(requestEventBus).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));
	}

	@Test
	public void shouldNotRescheduleIfMaxRequestTimeoutWillBeHit() throws Exception {

		timeProvider.forwardMillis(1950);

		requestEventBus.triggerEvent(Event.TOUCH);

		idleTimeoutTimerTask.run(null);


		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));

		verify(timeoutTimerHandler, times(0)).scheduleTimeout(anyInt(), eq(TimeUnit.MILLISECONDS));

	}

	@Test
	public void shouldRescheduleIfNotExpired() throws Exception {

		timeProvider.forwardMillis(75);

		idleTimeoutTimerTask.run(null);

		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));

		verify(timeoutTimerHandler).scheduleTimeout(25, TimeUnit.MILLISECONDS);

	}


	@Test
	public void shouldRescheduleIfTouched() throws Exception {

		timeProvider.forwardMillis(75);

		requestEventBus.triggerEvent(Event.TOUCH);

		timeProvider.forwardMillis(75);

		idleTimeoutTimerTask.run(null);

		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));

		verify(timeoutTimerHandler).scheduleTimeout( 25, TimeUnit.MILLISECONDS);

	}

	@Test
	public void shouldNotRunIfCompleted() throws Exception {
		idleTimeoutTimerTask.completed();

		timeProvider.forwardMillis(200);

		idleTimeoutTimerTask.run(null);

		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));
	}

	@Test
	public void shouldNotRunIfCanceled() throws Exception {
		idleTimeoutTimerTask.cancel();

		timeProvider.forwardMillis(200);

		idleTimeoutTimerTask.run(null);

		verify(requestEventBus, times(0)).triggerEvent(eq(Event.ERROR), eq(httpRequestContext), any(TimeoutException.class));
	}
}
