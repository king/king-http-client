// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;

public class Timeout {
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

	}

	@Test
	public void getShouldTriggerTimeout() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").totalRequestTimeoutMillis(200).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertNotNull(httpCallback.getException());

		assertEquals(null, httpCallback.getBody());

	}

	@Test
	public void idleGetShouldTriggerTimeout() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").idleTimeoutMillis(100).totalRequestTimeoutMillis(1000).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertNotNull(httpCallback.getException());

		assertEquals(null, httpCallback.getBody());

	}


	@Test
	public void getShouldNotTriggerTimeoutAfterCompletion() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").totalRequestTimeoutMillis(100).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertNull(httpCallback.getException());

		Thread.sleep(200);

		assertNull(httpCallback.getException());

	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
