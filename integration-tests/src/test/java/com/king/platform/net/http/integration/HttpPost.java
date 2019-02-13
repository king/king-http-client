// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpClientRequestWithBodyBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HttpPost {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur consectetur, mi at porta volutpat, mauris massa ";

	private AtomicReference<byte[]> readBodyContent;
	private AtomicReference<String> contentTypeValue;

	private HttpClientRequestWithBodyBuilder post;
	private BlockingHttpCallback httpCallback;


	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();


		readBodyContent = new AtomicReference<>();
		contentTypeValue = new AtomicReference<>();


		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				contentTypeValue.set(req.getContentType());

				byte[] body = integrationServer.readPostBody(req);
				readBodyContent.set(body);

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		post = httpClient.createPost("http://localhost:" + port + "/testOk");
		httpCallback = new BlockingHttpCallback();

	}

	@Test
	public void post200() throws Exception {
		post.build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
	}


	@Test
	public void postBodyWithByteArray() throws Exception {

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		post.content(content.getBytes(StandardCharsets.UTF_8)).build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();

		assertEquals(content, new String(readBodyContent.get(), StandardCharsets.UTF_8));
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void postBodyWithInputStream() throws Exception {
		post.content(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))).build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();

		assertEquals(content, new String(readBodyContent.get(), StandardCharsets.UTF_8));
		assertEquals(200, httpCallback.getStatusCode());

	}





	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
