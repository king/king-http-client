// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpClientRequestBuilder;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.NioCallback;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class HttpNioCallback {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@BeforeEach
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
		request.build().withHttpCallback(httpCallback).withNioCallback(nioCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


		InOrder order = inOrder(nioCallback);

		order.verify(nioCallback).onConnecting();
		order.verify(nioCallback).onConnected();
		order.verify(nioCallback).onWroteHeaders();
		order.verify(nioCallback).onWroteContentCompleted();
		order.verify(nioCallback).onReceivedStatus(any(HttpResponseStatus.class));
		order.verify(nioCallback).onReceivedHeaders(any(io.netty.handler.codec.http.HttpHeaders.class));
		order.verify(nioCallback).onReceivedContentPart(anyInt(), any(ByteBuf.class));
		order.verify(nioCallback).onReceivedCompleted(any(HttpResponseStatus.class), any(io.netty.handler.codec.http.HttpHeaders.class));
		verify(nioCallback, times(0)).onError(any(Throwable.class));

	}

	@Test
	public void getWithNioCallbackShouldDefaultToStringBodyConsumer() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		CompletableFuture<HttpResponse<String>> future = httpClient.createGet("http://localhost:" + port + "/testOk").build().withNioCallback(mock(NioCallback.class)).execute();

		HttpResponse<String> response = future.get();
		assertEquals(okBody, response.getBody());

	}

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
