// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpMethod;
import com.king.platform.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpDelete {
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
	public void delete200() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createDelete("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void delete404() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setStatus(404);
			}
		}, "/test404");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createDelete("http://localhost:" + port + "/test404").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());


	}

	@Test
	public void deleteWithPostBody() {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] body = integrationServer.readPostBody(req);
				resp.setStatus(200);
				resp.getOutputStream().write(body);
				resp.getOutputStream().flush();
			}
		}, "/testPost");


		HttpResponse<String> response = httpClient.create(HttpMethod.DELETE, "http://localhost:" + port + "/testPost")
			.content("HELLO WORLD".getBytes(StandardCharsets.UTF_8))
			.build()
			.execute()
			.join();

		assertEquals("HELLO WORLD", response.getBody());
		assertEquals(200, response.getStatusCode());

	}

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
