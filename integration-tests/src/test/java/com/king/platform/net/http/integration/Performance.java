// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class Performance {
	int NR_OF_RUNS = 400;

	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(NR_OF_RUNS * 2);
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new NettyHttpClientBuilder().setNioThreads(10).setHttpCallbackExecutorThreads(10).createHttpClient();
		httpClient.start();

	}

	@Test
	@Ignore
	public void getParallel() throws Exception {

		final AtomicInteger successCounter = new AtomicInteger();
		final AtomicInteger failCounter = new AtomicInteger();
		final CountDownLatch countDownLatch = new CountDownLatch(NR_OF_RUNS);

		final CountDownLatch serverCountDownLatch = new CountDownLatch(NR_OF_RUNS);
		final CountDownLatch serverLock = new CountDownLatch(1);

		final AtomicInteger parallelCounter = new AtomicInteger();
		final AtomicInteger maxParallelValue = new AtomicInteger();


		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				int i = parallelCounter.incrementAndGet();

				int max = maxParallelValue.get();
				if (i > max) {
					maxParallelValue.compareAndSet(max, i);
				}

				serverCountDownLatch.countDown();

				try {
					serverLock.await();
				} catch (InterruptedException e) {
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
				parallelCounter.decrementAndGet();
			}
		}, "/testOk");

		for (int i = 0; i < NR_OF_RUNS; i++) {
			httpClient.createGet("http://localhost:" + port + "/testOk").build().execute(new HttpCallback<String>() {
				@Override
				public void onCompleted(HttpResponse httpResponse) {
					if (httpResponse.getBody().equals(okBody)) {
						successCounter.incrementAndGet();
					} else {
						failCounter.incrementAndGet();

					}
					countDownLatch.countDown();
				}

				@Override
				public void onError(Throwable exception) {
					failCounter.incrementAndGet();
					exception.printStackTrace();
					countDownLatch.countDown();
				}
			});
		}

		serverCountDownLatch.await();
		serverLock.countDown();

		countDownLatch.await(2000, TimeUnit.MILLISECONDS);


		assertEquals("Nr of fail is not correct", 0, failCounter.get());
		assertEquals("Nr of success is not correct", NR_OF_RUNS, successCounter.get());
		assertEquals("Nr of parallel server side execution is not correct", NR_OF_RUNS, maxParallelValue.intValue());


	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
