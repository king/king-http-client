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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HttpPostWithInputStreamBody {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private String content = "BODY CONTENT";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}


	@Test
	public void postBody() throws Exception {
		final AtomicReference<String> bodyContent = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] body = readPostBody(req);
				bodyContent.set(new String(body));

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").content(content.getBytes()).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertEquals(content, bodyContent.get());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void postBodyWithContentType() throws Exception {
		final AtomicReference<String> contentTypeValue = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				contentTypeValue.set(req.getContentType());

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		String contentType = "text/unit test";

		httpClient.createPost("http://localhost:" + port + "/testOk").content(content.getBytes()).contentType(contentType).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertEquals(contentType, contentTypeValue.get());

	}

	private byte[] readPostBody(HttpServletRequest req) throws IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			byte[] data = new byte[4096];
			int bytesRead;
			while ((bytesRead = req.getInputStream().read(data, 0, data.length)) >= 0) {
				baos.write(data, 0, bytesRead);
			}

			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
