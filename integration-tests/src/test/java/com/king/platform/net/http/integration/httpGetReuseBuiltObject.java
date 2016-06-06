// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.BuiltClientRequest;
import com.king.platform.net.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class httpGetReuseBuiltObject {
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
	public void getTwiceWithSame() throws Exception {
		final AtomicInteger counter = new AtomicInteger();
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody + counter.incrementAndGet());
				resp.getWriter().flush();
			}
		}, "/testOk");

		BuiltClientRequest builtClientRequest = httpClient.createGet("http://localhost:" + port + "/testOk").build();

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		builtClientRequest.execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okBody + "1", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


		httpCallback = new BlockingHttpCallback();
		builtClientRequest.execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okBody + "2", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void getInParallel() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.setStatus(200);
			}
		}, "/testOk");

		final BuiltClientRequest builtClientRequest = httpClient.createGet("http://localhost:" + port + "/testOk").build();

		final AtomicBoolean failed = new AtomicBoolean();

		int threads = 10;

		final CountDownLatch startupLatch = new CountDownLatch(threads);
		final CountDownLatch waitLatch = new CountDownLatch(1);
		final CountDownLatch completedLatch = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					startupLatch.countDown();

					try {
						waitLatch.await();
					} catch (InterruptedException ignored) {
					}

					BlockingHttpCallback httpCallback = new BlockingHttpCallback();
					builtClientRequest.execute(httpCallback);
					try {
						httpCallback.waitForCompletion();
						if (httpCallback.getStatusCode() != 200) {
							failed.set(true);
						}
					} catch (InterruptedException ignored) {

					}

					completedLatch.countDown();
				}
			}).start();
		}


		startupLatch.await();
		waitLatch.countDown();
		completedLatch.await();

		assertFalse(failed.get());


	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
