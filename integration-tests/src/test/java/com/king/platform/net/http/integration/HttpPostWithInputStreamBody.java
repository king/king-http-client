// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressivePromise;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpPostWithInputStreamBody {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private byte[] content;

	@Before
	public void setUp() throws Exception {
		content = new byte[1024 * 16];
		new Random().nextBytes(content);

		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}


	@Test
	public void postBody() throws Exception {
		final AtomicReference<byte[]> bodyContent = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] body = readPostBody(req);
				bodyContent.set(body);
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").content(content).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertArrayEquals(content, bodyContent.get());
		assertEquals(200, httpCallback.getStatusCode());


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

	@Test
	public void postBodyAsStream() throws Exception {
		final AtomicReference<byte[]> bodyContent = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] body = readPostBody(req);
				bodyContent.set(body);

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").content(new ByteArrayInputStream(content)).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertArrayEquals(content, bodyContent.get());
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void postBodyCustomHttpBody() throws Exception {
		final AtomicReference<byte[]> bodyContent = new AtomicReference<>();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] body = readPostBody(req);
				bodyContent.set(body);

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createPost("http://localhost:" + port + "/testOk").content(new HttpBody() {
			@Override
			public long getContentLength() {
				return content.length;
			}

			@Override
			public String getContentType() {
				return "application/binary";
			}

			@Override
			public Charset getCharacterEncoding() {
                return StandardCharsets.ISO_8859_1;
            }

			@Override
			public ChannelFuture writeContent(final ChannelHandlerContext ctx) throws IOException {
				final ChannelProgressivePromise promise = ctx.newProgressivePromise();
				promise.setProgress(0, content.length);
				new Thread(new Runnable() {
					@Override
					public void run() {
						int index = 0;
						int length = 1024;
						while (true) {
							ByteBuf byteBuf = ctx.alloc().buffer(length).writeBytes(content, index, length);
							ctx.writeAndFlush(byteBuf).awaitUninterruptibly();
							index += length;
							if (index >= content.length) {
								break;
							}
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							promise.setProgress(index, content.length);
						}

						promise.setSuccess();
					}
				}).start();

				return promise;
			}
		}).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertArrayEquals(content, bodyContent.get());
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

		httpClient.createPost("http://localhost:" + port + "/testOk").content(content).contentType(contentType).build().execute(httpCallback);

		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());
		assertTrue(contentTypeValue.get().startsWith(contentType));

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
