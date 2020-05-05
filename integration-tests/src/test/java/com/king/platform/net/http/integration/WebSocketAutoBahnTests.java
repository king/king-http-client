// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.WebSocketConnection;
import com.king.platform.net.http.WebSocketListener;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.backpressure.EvictingBackPressure;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

@Disabled
public class WebSocketAutoBahnTests {
	private HttpClient httpClient;

	@BeforeEach
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

	for (int i = 1; i < 310; i++) {

			System.out.println("Starting "+ i );

			CountDownLatch countDownLatch = new CountDownLatch(1);
			httpClient.createWebSocket("ws://localhost:" + 9001 + "/runCase?case="+i+"&agent=king-http-client")
				.splitLargeFrames(false)
				.maxFrameSize(1024*1024*16)
				.maxAggregateBufferSize(1024*1024*64)
				.build()

				.execute(new WebSocketListener() {
					WebSocketConnection client;

					@Override
					public void onConnect(WebSocketConnection connection) {
						this.client = connection;
					}

					@Override
					public void onCloseFrame(int code, String reason) {

					}

					@Override
					public void onError(Throwable throwable) {
						System.out.println("Client error " + throwable);
						countDownLatch.countDown();
					}

					@Override
					public void onDisconnect() {
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
		CountDownLatch countDownLatch = new CountDownLatch(1);
		httpClient.createWebSocket("ws://127.0.0.1:9001/updateReports?agent=king-http-client").build().execute(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				System.out.println("Connected");
			}

			@Override
			public void onCloseFrame(int code, String reason) {
				countDownLatch.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				countDownLatch.countDown();
			}

			@Override
			public void onDisconnect() {
				countDownLatch.countDown();
			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {
				System.out.println(payload);
			}
		});
		countDownLatch.await();
	}

	@AfterEach
	public void tearDown() throws Exception {
		httpClient.shutdown();
	}


}
