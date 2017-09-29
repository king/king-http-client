// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.ConnectionClosedException;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HttpGetWithCallback {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(5000);
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}


	@Test
	public void get200WithClose() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


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
		httpClient.createGet("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void get200WithContentLength() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");

				ServletOutputStream outputStream = resp.getOutputStream();
				byte[] bytes = okBody.getBytes();

				resp.setContentLength(bytes.length);

				outputStream.write(bytes);
				outputStream.flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

	}


	@Test
	public void get200WithIncorrectContentLength() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");

				ServletOutputStream outputStream = resp.getOutputStream();
				byte[] bytes = okBody.getBytes();

				resp.setContentLength(2000);

				outputStream.write(bytes);
				outputStream.flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertNotNull(httpCallback.getException());
		assertTrue(httpCallback.getException() instanceof ConnectionClosedException);


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
		httpClient.createGet("http://localhost:" + port + "/test404").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());


	}

	@Test
	public void getWithCustomExecutor() throws Exception {
		String poolName = "unit-test-executors";
		ExecutorService executorService = Executors.newFixedThreadPool(1, new DefaultThreadFactory(poolName));

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		final AtomicReference<String> threadName = new AtomicReference<>();
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		httpClient.createGet("http://localhost:" + port + "/testOk").executingOn(executorService).build().withHttpCallback(new HttpCallback<String>() {
			@Override
			public void onCompleted(HttpResponse<String> httpResponse) {
				threadName.set(Thread.currentThread().getName());
				countDownLatch.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				countDownLatch.countDown();
			}
		}).execute();
		countDownLatch.await();

		assertTrue(threadName.get().startsWith(poolName));
	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
