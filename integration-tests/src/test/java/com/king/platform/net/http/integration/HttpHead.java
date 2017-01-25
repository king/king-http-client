// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.ConnectionClosedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;

public class HttpHead {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okHeader = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(5000);
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}


	@Test
	public void head200WithClose() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.addHeader("X-OK", okHeader);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void head200() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("X-OK", okHeader);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void head200WithContentLength() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.addHeader("X-OK", okHeader);
				resp.setContentLength(512);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals("", httpCallback.getBody());
		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("512", httpCallback.getHeader("content-length"));
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void get404() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setStatus(404);
			}
		}, "/test404");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/test404").build().execute(httpCallback);
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());


	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
