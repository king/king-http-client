// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HttpGetWithFuture {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


	}

	@Test
	public void get200() throws Exception {

		CountDownLatch countDownLatch = new CountDownLatch(1);
		CompletableFuture<HttpResponse<String>> resultFuture = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute();
		resultFuture.whenComplete((httpResponse, throwable) -> {
			assertEquals(okBody, httpResponse.getBody());
			assertEquals(200, httpResponse.getStatusCode());
			countDownLatch.countDown();
        });

		countDownLatch.await();

	}

	@Test
	public void getUnknownHost() throws Exception {

		CompletableFuture<HttpResponse<String>> resultFuture = httpClient.createGet("http://loasdwd.calhost:" + port + "/testOk").build().execute();

		try {
			resultFuture.get(4, TimeUnit.SECONDS);
			fail("Should have thrown exception");
		}catch (ExecutionException ee) {
			Class<? extends Throwable> aClass = ee.getCause().getClass();
			assertEquals(UnknownHostException.class, aClass);
		}
	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
