// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static se.mockachino.Mockachino.mock;


public class ResponseFutureTest {
	private RequestEventBus requestEventBus;
	private HttpRequestContext requestContext;
	private ResponseFuture responseFuture;

	@Before
	public void setUp() throws Exception {
		requestContext = mock(HttpRequestContext.class);
		requestEventBus = new DefaultEventBus();
		responseFuture = new ResponseFuture(requestEventBus, requestContext);
	}

	@Test
	public void doneShouldCompleteTheFuture() throws Exception {
		HttpResponse httpResponse = mock(HttpResponse.class);
		requestEventBus.triggerEvent(Event.onHttpResponseDone, httpResponse);
		assertTrue(responseFuture.isDone());
		FutureResult futureResult = responseFuture.get(1, TimeUnit.MILLISECONDS);
		assertSame(httpResponse, futureResult.getHttpResponse());
	}


	@Test
	public void errorShouldCompleteTheFuture() throws Exception {
		Throwable t = new Exception();
		requestEventBus.triggerEvent(Event.ERROR, requestContext, t);
		assertTrue(responseFuture.isDone());
		FutureResult futureResult = responseFuture.get(1, TimeUnit.MILLISECONDS);
		assertSame(t, futureResult.getError());
	}

	@Test
	public void cancelShouldCompleteTheFuture() throws Exception {
		responseFuture.cancel(true);
		assertTrue(responseFuture.isDone());
		assertTrue(responseFuture.isCancelled());
		FutureResult futureResult = responseFuture.get(1, TimeUnit.MILLISECONDS);
		assertNotNull(futureResult.getError());
	}

	@Test
	public void factoryMethodForErrorShouldReturnDoneFuture() throws Exception {
		Exception exception = new Exception();
		ResponseFuture future = ResponseFuture.error(exception);
		assertTrue(future.isDone());
		assertFalse(future.isCancelled());
		FutureResult futureResult = future.get(1, TimeUnit.MILLISECONDS);
		assertSame(exception, futureResult.getError());

	}
}
