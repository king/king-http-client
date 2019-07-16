// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HttpPostConteType {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur consectetur, mi at porta volutpat, mauris massa ";

	private AtomicReference<byte[]> readBodyContent;
	private AtomicReference<String> contentTypeValue;
	private AtomicReference<String> characterEncoding;

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
		characterEncoding = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				contentTypeValue.set(req.getContentType());
				characterEncoding.set(req.getCharacterEncoding());

				byte[] body = readPostBody(req);
				readBodyContent.set(body);

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		post = httpClient.createPost("http://localhost:" + port + "/testOk");
		httpCallback = new BlockingHttpCallback();

	}


	@Test
	public void postBodyWithContentTypeAndNoEncoding() throws Exception {
		String contentType = "text/unitTest";

		post.content(content.getBytes(StandardCharsets.UTF_8)).contentType(contentType).build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();

		assertTrue(contentTypeValue.get().startsWith(contentType));
		assertEquals(StandardCharsets.ISO_8859_1.displayName().toLowerCase(), characterEncoding.get().toLowerCase());

	}


	@Test
	public void postBodyWithContentTypeAndEncodingInContentType() throws Exception {
		String contentType = "text/unitTest;charset=utf-8";

		post.content(content.getBytes(StandardCharsets.UTF_8)).contentType(contentType).build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();


		assertTrue(contentTypeValue.get().startsWith(contentType));
		assertEquals("UTF-8".toLowerCase(), characterEncoding.get().toLowerCase());
	}

	@Test
	public void postBodyWithContentTypeAndEncoding() throws Exception {
		String contentType = "text/unitTest";

		post.content(content.getBytes(StandardCharsets.UTF_8))
			.bodyCharset(StandardCharsets.UTF_8)
			.contentType(contentType)
			.build()
			.withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertTrue(contentTypeValue.get().startsWith(contentType));
		assertEquals("UTF-8".toLowerCase(), characterEncoding.get().toLowerCase());
	}

	@Test
	public void postBodyWithNoContentTypeAndEncoding() throws Exception {
		post.content(content.getBytes(StandardCharsets.UTF_8))
			.bodyCharset(StandardCharsets.UTF_8)
			//.contentType(null)
			.build()
			.withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertNull(contentTypeValue.get());
		assertNull( characterEncoding.get());
	}

	@Test
	public void postBodyWithContentTypeAndDualEncoding() throws Exception {
		String contentType = "text/unitTest;charset=utf-8";
		post.content(content.getBytes(StandardCharsets.UTF_8))
			.bodyCharset(StandardCharsets.US_ASCII)
			.contentType(contentType)
			.build()
			.withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertTrue(contentTypeValue.get().startsWith(contentType));
		assertEquals("UTF-8".toLowerCase(), characterEncoding.get().toLowerCase());
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
