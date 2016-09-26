// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.Event;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpSse {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(5000);
		integrationServer.start();
		port = integrationServer.getPort();

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		recordingEventBus = testingHttpClientFactory.getRecordingEventBus();
		httpClient = testingHttpClientFactory.create();
		httpClient.start();

	}

	@Test
	public void getSSE() throws Exception {

		integrationServer.addServlet(new MyEventSourceServlet(), "/testSSE");

		AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new HttpSseCallback() {
			String buffer = "";

			@Override
			public void onConnect() {

			}

			@Override
			public void onDisconnect() {
				output.set(buffer);
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				buffer = buffer + data;
			}
		});

		sseClient.awaitClose(); //block until complete

		assertEquals("0123456789", output.get());


	}

	@Test
	public void getSseAndClose() throws Exception {
		integrationServer.addServlet(new MyEventSourceServlet(), "/testSSE");

		CountDownLatch countDownLatch = new CountDownLatch(2);
		AtomicInteger counter = new AtomicInteger();
		AtomicBoolean onError = new AtomicBoolean();
		AtomicBoolean onConnect = new AtomicBoolean();
		AtomicBoolean onDisconnect = new AtomicBoolean();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new HttpSseCallback() {

			@Override
			public void onConnect() {
				onConnect.set(true);
			}

			@Override
			public void onDisconnect() {
				onDisconnect.set(true);
			}

			@Override
			public void onError(Throwable throwable) {
				onError.set(true);
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				counter.incrementAndGet();
				countDownLatch.countDown();

			}
		});

		countDownLatch.await();

		sseClient.close();
		Thread.sleep(400);

		assertEquals(2, counter.get());

		assertTrue(onConnect.get());
		assertTrue(onDisconnect.get());
		assertFalse(onError.get());

		assertEquals(1, recordingEventBus.getTriggeredCount(Event.COMPLETED));

	}

	@Test
	public void reconnect() throws Exception {
		integrationServer.addServlet(new MyEventSourceServlet(), "/testSSE");

		AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new HttpSseCallback() {
			String buffer = "";

			@Override
			public void onConnect() {

			}

			@Override
			public void onDisconnect() {
				output.set(buffer);
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				buffer = buffer + data;
			}
		});

		sseClient.awaitClose();

		assertEquals("0123456789", output.get());

		sseClient.reconnect();

		sseClient.awaitClose();
		assertEquals("01234567890123456789", output.get());


	}



	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


	private static class MyEventSourceServlet extends EventSourceServlet {

		@Override
		protected EventSource newEventSource(HttpServletRequest request) {
			return new EventSource() {
				@Override
				public void onOpen(final Emitter emitter) throws IOException {
					new Thread(new Runnable() {
						@Override
						public void run() {
							for (int i = 0; i < 10; i++) {
								try {
									emitter.data("" + i);
									Thread.sleep(100);
								} catch (IOException e) {
									return;
								} catch (InterruptedException e) {

								}
							}
							emitter.close();
						}
					}).start();
				}

				@Override
				public void onClose() {

				}
			};
		}
	}
}
