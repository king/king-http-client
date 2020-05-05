// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpClientRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpGetWithParameters {
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
	public void getWithParameters() throws Exception {
		final AtomicReference<String> value1 = new AtomicReference<>();
		final AtomicReference<String> value2 = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				value1.set(req.getParameter("param1"));
				value2.set(req.getParameter("param2"));
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient
			.createGet("http://localhost:" + port + "/testOk")
			.addQueryParameter("param1", "value1")
			.addQueryParameter("param2", "value2")
			.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertEquals("value1", value1.get());
		assertEquals("value2", value2.get());

	}

	@Test
	public void getWithBakedUrlParameters() throws Exception {
		final AtomicReference<String> value1 = new AtomicReference<>();
		final AtomicReference<String> value2 = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				value1.set(req.getParameter("param1"));
				value2.set(req.getParameter("param2"));
				resp.getWriter()
					.write(okBody);
				resp.getWriter()
					.flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk?param1=value1&param2=value2")
			.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertEquals("value1", value1.get());
		assertEquals("value2", value2.get());

	}


	@Test
	public void getWithParameterInUrlAndQueryParams() throws Exception {
		final AtomicReference<String> value0 = new AtomicReference<>();
		final AtomicReference<String> value1 = new AtomicReference<>();
		final AtomicReference<String> value2 = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				value0.set(req.getParameter("param0"));
				value1.set(req.getParameter("param1"));
				value2.set(req.getParameter("param2"));
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient
			.createGet("http://localhost:" + port + "/testOk?param0=value0")
			.addQueryParameter("param1", "value1")
			.addQueryParameter("param2", "value2")
			.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertEquals("value0", value0.get());
		assertEquals("value1", value1.get());
		assertEquals("value2", value2.get());

	}

	@Test
	public void getWithEncodedParameters() throws Exception {

		final String[] values = new String[]{
			"abcdefghijklmnopqrstuvwxyz",
			"ABCDEFGHIJKQLMNOPQRSTUVWXYZ",
			"1234567890", "1234567890",
			"`~!@#$%^&*()", "`~!@#$%^&*()",
			"_+-=,.<>/?", "_+-=,.<>/?",
			";:'\"[]{}\\| ", ";:'\"[]{}\\| "
		};

		final String[] result = new String[values.length];

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

				for (int i = 0; i < values.length; i++) {
					result[i] = req.getParameter("param" + i);
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		HttpClientRequestBuilder request = httpClient.createGet("http://localhost:" + port + "/testOk");

		for (int i = 0; i < values.length; i++) {
			request.addQueryParameter("param" + i, values[i]);
		}

		request.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


		for (int i = 0; i < values.length; i++) {
			assertEquals(values[i], result[i]);
		}


	}

	@Test
	public void getWithParameterMap() throws Exception {
		final AtomicReference<String> value1 = new AtomicReference<>();
		final AtomicReference<String> value2 = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				value1.set(req.getParameter("param1"));
				value2.set(req.getParameter("param2"));
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		Map<String, String> parameterMap = new HashMap<>();
		parameterMap.put("param1", "value1");
		parameterMap.put("param2", "value2");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient
			.createGet("http://localhost:" + port + "/testOk")
			.addQueryParameters(parameterMap)
			.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertEquals("value1", value1.get());
		assertEquals("value2", value2.get());

	}

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}

