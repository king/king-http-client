// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.WebSocketClient;
import com.king.platform.net.http.WebSocketClientCallback;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.backpressure.EvictingBackPressure;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import com.sun.org.apache.xpath.internal.SourceTree;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertEquals;

public class WebSocketAutoBahnTests {
	private HttpClient httpClient;

	@Before
	public void setUp() throws Exception {

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
	public void webSocket() throws Exception {

		;

	for (int i = 1; i < 400; i++) {

			System.out.println("Starting "+ i );

			CountDownLatch countDownLatch = new CountDownLatch(1);
			httpClient.createWebSocket("ws://localhost:" + 9001 + "/runCase?case="+i+"&agent=king-http-client")
				.executingOn(Executors.newSingleThreadExecutor())
				.build()

				.execute(new WebSocketClientCallback() {
					WebSocketClient client;

					@Override
					public void onConnect(WebSocketClient client) {
						this.client = client;
					}

					@Override
					public void onDisconnect(int code, String reason) {
						countDownLatch.countDown();
					}

					@Override
					public void onError(Throwable t) {
						System.out.println("Client error " + t);
						countDownLatch.countDown();
					}

					@Override
					public void onTextFrame(String payload, boolean finalFragment, int rsv) {
						client.sendTextFrame(payload);
					}

					@Override
					public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
						client.sendBinaryFrame(payload);
					}


				});

			countDownLatch.await();
			System.out.println("Completed "+ i );
		}




	}

	@Test
	public void updateReport() throws Exception {
		httpClient.createWebSocket("ws://127.0.0.1:9001/updateReports?agent=king-http-client").build().execute(new WebSocketClientCallback() {
			@Override
			public void onConnect(WebSocketClient client) {
				System.out.println("Connected");
			}

			@Override
			public void onDisconnect(int code, String reason) {
				System.exit(0);
			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {
				System.out.println(payload);
			}
		});
		Thread.currentThread().join();
	}

	@After
	public void tearDown() throws Exception {
		httpClient.shutdown();
	}


}
