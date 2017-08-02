// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.backpressure.EvictingBackPressure;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;

public class BackPressure {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(500);
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2)
			.setChannelPool(new NoChannelPool()).setExecutionBackPressure(new EvictingBackPressure(10))
			.setOption(ConfKeys.IDLE_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS, 0)
			.createHttpClient();


		httpClient.start();

	}

	@Test
	public void getMoreConcurrentThenAllowed() throws Exception {
		int NR_OF_RUNS = 10;


		final CountDownLatch goLatch = new CountDownLatch(1);

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				try {
					goLatch.await();
				} catch (InterruptedException ignored) {

				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}

				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		for (int i = 0; i < NR_OF_RUNS; i++) {
			httpClient.createGet("http://localhost:" + port + "/testOk").build().execute();
		}

		goLatch.countDown();

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().execute(httpCallback);

		httpCallback.waitForCompletion();


		assertNotNull(httpCallback.getException());


	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
