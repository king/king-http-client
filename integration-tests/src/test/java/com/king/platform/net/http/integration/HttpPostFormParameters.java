// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.netty.NettyHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HttpPostFormParameters {
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
	public void postOneParameter() throws Exception {
		String value = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKQLMNOPQRSTUVWXYZ1234567809`~!@#$%^&*()_+-=,.<>/?;:'\"[]{}\\| ";

		final AtomicReference<String> param1Value = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				param1Value.set(req.getParameter("param1"));
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").addFormParameter("param1", value).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

		assertEquals(value, param1Value.get());


	}

	@Test
	public void postMapOfParameters() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		parameters.put("param1", "value1");
		parameters.put("param2", "value2");
		parameters.put("param3", "value3");

		final Map<String, String> postedValues = new HashMap<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

				for (String key : parameters.keySet()) {
					postedValues.put(key, req.getParameter(key));
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").addFormParameters(parameters).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

		assertEquals(parameters, postedValues);

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
