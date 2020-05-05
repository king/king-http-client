// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpsGet {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@BeforeEach
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.startHttps();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory()
			.setOption(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES, true)
			.create();

		httpClient.start();

	}


	@Test
	public void get200() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("https://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

	}


	@Test
	public void get404() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setStatus(404);
			}
		}, "/test404");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("https://localhost:" + port + "/test404").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());

	}


	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
