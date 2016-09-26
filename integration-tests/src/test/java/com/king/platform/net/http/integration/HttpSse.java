// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpSseCallback;
import com.king.platform.net.http.SseClient;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HttpSse {
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
	public void getSSE() throws Exception {

		integrationServer.addServlet(new EventSourceServlet() {

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
		}, "/testSSE");

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



	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
