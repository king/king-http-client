// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Headers {
	private final String headerName = "X-Test-Header";
	private final String headerValue = "clientSuppliedHeader";
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
	public void getWithProvidedHeader() throws Exception {

		final AtomicReference<String> headerValueReference = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				headerValueReference.set(req.getHeader(headerName));

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		HttpResponse<String> response = httpClient
			.createGet("http://localhost:" + port + "/testOk")
			.addHeader(headerName, headerValue)
			.build()
			.execute()
			.get(1, TimeUnit.SECONDS);


		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());
		assertEquals(headerValue, headerValueReference.get());

	}

	@Test
	public void getWithServerHeader() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setHeader(headerName, headerValue);

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		HttpResponse<String> response = httpClient.createGet("http://localhost:" + port + "/testOk")
			.build()
			.execute()
			.get(1, TimeUnit.SECONDS);


		assertEquals(okBody, response.getBody());
		assertEquals(headerValue, response.getHeader(headerName));
		assertEquals(200, response.getStatusCode());


	}

	@Test
	public void getWithProvidedHeaderMap() throws Exception {

		final AtomicReference<String> headerValueReference = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				headerValueReference.set(req.getHeader(headerName));

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");


		HashMap<CharSequence, CharSequence> headers = new HashMap<>();
		headers.put(headerName, headerValue);

		HttpResponse<String> response = httpClient
			.createGet("http://localhost:" + port + "/testOk")
			.addHeaders(headers)
			.build()
			.execute()
			.get(1, TimeUnit.SECONDS);


		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());
		assertEquals(headerValue, headerValueReference.get());

	}


	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
