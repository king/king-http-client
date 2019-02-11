// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.backpressure.EvictingBackPressure;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

public class WebSocketTimeouts {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(500);
		integrationServer.start();
		port = integrationServer.getPort();

		recordingEventBus = new RecordingEventBus(new DefaultEventBus());

		httpClient = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2)
			.setChannelPool(new NoChannelPool()).setExecutionBackPressure(new EvictingBackPressure(10))
			.setOption(ConfKeys.IDLE_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS, 0)
			.setRootEventBus(recordingEventBus)
			.createHttpClient();


		httpClient.start();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.sendRedirect("ws://localhost:" + port + "/websocket/test");
			}
		}, "/redirect");

		integrationServer.addServlet(new WebSocketServlet() {
			@Override
			public void configure(WebSocketServletFactory factory) {

				factory.register(EchoWebSocketEndpoint.class);
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				try {
					System.out.println("About to sleep servlet for 5s!");
					Thread.sleep(800);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				super.service(request, response);
			}
		}, "/websocket/test");


	}

	public static class EchoWebSocketEndpoint implements org.eclipse.jetty.websocket.api.WebSocketListener {

		private Session session;

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {

		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
		}

		@Override
		public void onWebSocketConnect(Session session) {
			this.session = session;
		}

		@Override
		public void onWebSocketError(Throwable cause) {
		}

		@Override
		public void onWebSocketText(String message) {
			try {
				session.getRemote().sendString(message.toUpperCase());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test(timeout = 10000)
	public void webSocketWithTimeoutOnUpgrade() throws Exception {



		BuiltWebSocketRequest builtWebSocketRequest = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build();


		AtomicBoolean connected = new AtomicBoolean();

		WebSocketClient webSocketClient = builtWebSocketRequest.build();
		webSocketClient.addListener(new WebSocketListenerAdapter() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				connected.set(true);
				webSocketClient.close();
			}
		});

		try {
			webSocketClient.connect().get(200, TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException to) {
			webSocketClient.close();
		}
		recordingEventBus.waitFor(Event.COMPLETED);


		assertFalse(connected.get());

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
