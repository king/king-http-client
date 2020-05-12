// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ResponseFutureTest {
	private RequestEventBus requestEventBus;
	private HttpRequestContext requestContext;
	private ResponseFuture<HttpResponse> responseFuture;

	@BeforeEach
	public void setUp() throws Exception {
		requestContext = mock(HttpRequestContext.class);
		requestEventBus = new DefaultEventBus();
		responseFuture = new ResponseFuture<>(requestEventBus, requestContext, Runnable::run);
	}

	@Test
	public void doneShouldCompleteTheFuture() throws Exception {
		HttpResponse httpResponse = mock(HttpResponse.class);
		HttpRequestContext context = mock(HttpRequestContext.class);
		when(context.getHttpResponse()).thenReturn(httpResponse);
		requestEventBus.triggerEvent(Event.COMPLETED, context);
		assertTrue(responseFuture.isDone());
		HttpResponse<HttpResponse> futureResult = responseFuture.get(1, TimeUnit.MILLISECONDS);
		assertSame(httpResponse, futureResult);
	}


	@Test
	public void errorShouldCompleteTheFuture() throws Exception {
		Throwable t = new Exception();
		requestEventBus.triggerEvent(Event.ERROR, requestContext, t);
		assertTrue(responseFuture.isDone());

		try {
			responseFuture.get(1, TimeUnit.MILLISECONDS);
			fail("should have thrown exception");
		} catch (ExecutionException ee) {
			assertSame(t, ee.getCause());
		}
	}


	@Test
	public void cancelShouldCompleteTheFuture() throws Exception {
		responseFuture.cancel(true);
		assertTrue(responseFuture.isDone());
		assertTrue(responseFuture.isCancelled());
		try {
			responseFuture.get();
			fail("Should have thrown exception");
		} catch (CancellationException e) {
			assertNotNull(e);
		}

	}

	@Test
	public void factoryMethodForErrorShouldReturnDoneFuture() throws Exception {
		Exception exception = new Exception();
		CompletableFuture<HttpResponse<String>> future = ResponseFuture.error(exception);

		assertTrue(future.isDone());
		assertFalse(future.isCancelled());
		try {
			future.get(1, TimeUnit.MILLISECONDS);
			fail("should have thrown exception");
		} catch (ExecutionException ee) {
			assertSame(exception, ee.getCause());
		}

	}
}
