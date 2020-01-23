// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.NioCallbackAdapter;
import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class Exceptions {
	private final String headerName = "X-Test-Header";
	private final String headerValue = "clientSuppliedHeader";
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private byte[] okBody = new byte[1024 * 1024];


	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();
		new Random().nextBytes(okBody);
	}

	@Test
	public void getWithExceptionInBodyConsumer() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getOutputStream().write(okBody);
				resp.getOutputStream().flush();
			}
		}, "/getData");

		AtomicReference<Throwable> nioException = new AtomicReference<>();
		String exceptionMessage = "Failed to buffer!";
		;

		CompletableFuture<HttpResponse<Void>> completableFuture = httpClient.createGet("http://localhost:" + port + "/getData")
			.build(() -> new ResponseBodyConsumer<Void>() {
				@Override
				public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
				}

				@Override
				public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
					throw new RuntimeException(exceptionMessage);
				}

				@Override
				public void onCompletedBody() throws Exception {
				}

				@Override
				public Void getBody() {
					return null;
				}
			})
			.withNioCallback(new NioCallbackAdapter() {
				@Override
				public void onError(Throwable throwable) {
					nioException.set(throwable);
				}
			})
			.execute();

		CountDownLatch countDownLatch = new CountDownLatch(1);

		completableFuture.whenComplete((voidHttpResponse, throwable) -> countDownLatch.countDown());
		countDownLatch.await();

		assertTrue(completableFuture.isCompletedExceptionally());
		try {
			completableFuture.get();
			fail("Should have thrown exception!");
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			assertEquals(cause.getMessage(), exceptionMessage);
		}

		assertNotNull(nioException.get());
		assertEquals(nioException.get().getMessage(), exceptionMessage);
	}

	@Test
	public void postWithExceptionInBodyUpload() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				integrationServer.readPostBody(req);
				resp.getWriter().write("OK");
				resp.getWriter().flush();
			}
		}, "/postData");

		AtomicReference<Throwable> nioException = new AtomicReference<>();
		String exceptionMessage = "Failed to write body";

		CompletableFuture<HttpResponse<String>> completableFuture = httpClient.createPost("http://localhost:" + port + "/postData")
			.content(new HttpBody() {
				@Override
				public long getContentLength() {
					return okBody.length;
				}

				@Override
				public String getContentType() {
					return "application/binary";
				}

				@Override
				public Charset getCharacterEncoding() {
					return null;
				}

				@Override
				public ChannelFuture writeContent(ChannelHandlerContext ctx, boolean isSecure) throws IOException {
					throw new RuntimeException(exceptionMessage);
				}
			})
			.build()
			.withNioCallback(new NioCallbackAdapter() {
				@Override
				public void onError(Throwable throwable) {
					nioException.set(throwable);
				}
			})
			.execute();

		CountDownLatch countDownLatch = new CountDownLatch(1);

		completableFuture.whenComplete((voidHttpResponse, throwable) -> countDownLatch.countDown());
		countDownLatch.await();

		assertTrue(completableFuture.isCompletedExceptionally());
		try {
			completableFuture.get();
			fail("Should have thrown exception!");
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			assertEquals(cause.getMessage(), exceptionMessage);
		}

		assertNotNull(nioException.get());
		assertEquals(nioException.get().getMessage(), exceptionMessage);

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
