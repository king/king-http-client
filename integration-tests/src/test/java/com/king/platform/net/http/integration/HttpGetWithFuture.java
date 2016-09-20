// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.StringResponseBody;
import com.king.platform.net.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

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


		Future<FutureResult<String>> resultFuture = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute(new StringResponseBody());

		FutureResult<String> stringFutureResult = resultFuture.get(1, TimeUnit.SECONDS);

		HttpResponse<String> httpResponse = stringFutureResult.getHttpResponse();

		assertTrue(stringFutureResult.completed());

		assertEquals(okBody, httpResponse.getBody());
		assertEquals(200, httpResponse.getStatusCode());


	}

	@Test
	public void getUnknownHost() throws Exception {

		Future<FutureResult<String>> resultFuture = httpClient.createGet("http://loasdwd.calhost:" + port + "/testOk").build().execute(new StringResponseBody());

		FutureResult<String> stringFutureResult = resultFuture.get(4, TimeUnit.SECONDS);
		assertFalse(stringFutureResult.completed());
		assertTrue(stringFutureResult.failed());
		Throwable error = stringFutureResult.getError();
		assertNotNull(error);

	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
