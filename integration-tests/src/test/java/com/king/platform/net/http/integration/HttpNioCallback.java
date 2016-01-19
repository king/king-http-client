// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClientRequestBuilder;
import com.king.platform.net.http.NioCallback;
import com.king.platform.net.http.netty.NettyHttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import se.mockachino.order.OrderingContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static se.mockachino.Mockachino.*;
import static se.mockachino.matchers.Matchers.any;
import static se.mockachino.matchers.Matchers.anyInt;

public class HttpNioCallback {
	IntegrationServer integrationServer;
	private NettyHttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}

	@Test
	public void getWithNioCallbacks() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		NioCallback nioCallback = mock(NioCallback.class);

		HttpClientRequestBuilder request = httpClient.createGet("http://localhost:" + port + "/testOk");
		request.build().execute(httpCallback, nioCallback);
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

		OrderingContext order = newOrdering();

		order.verify().on(nioCallback).onConnecting();
		order.verify().on(nioCallback).onConnected();
		order.verify().on(nioCallback).onWroteHeaders();
		order.verify().on(nioCallback).onWroteContentCompleted();
		order.verify().on(nioCallback).onReceivedStatus(any(HttpResponseStatus.class));
		order.verify().on(nioCallback).onReceivedHeaders(any(io.netty.handler.codec.http.HttpHeaders.class));
		order.verify().on(nioCallback).onReceivedContentPart(anyInt(), any(ByteBuf.class));
		order.verify().on(nioCallback).onReceivedCompleted(any(HttpResponseStatus.class), any(io.netty.handler.codec.http.HttpHeaders.class));
		verifyNever().on(nioCallback).onError(any(Throwable.class));

	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
