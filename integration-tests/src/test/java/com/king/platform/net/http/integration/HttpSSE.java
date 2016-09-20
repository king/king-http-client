// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpSSECallback;
import com.king.platform.net.http.NioCallback;
import com.king.platform.net.http.netty.NettyHttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HttpSSE {
	IntegrationServer integrationServer;
	private NettyHttpClient httpClient;
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

		Future<FutureResult<Void>> sseResult = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new HttpSSECallback() {
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

		sseResult.get(); //block until complete

		assertEquals("0123456789", output.get());


	}



	@Test
	public void get200() throws Exception {

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
										emitter.data("from event " + i);
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


		httpClient.createGet("http://localhost:" + port + "/testSSE").withHeader("Accept", "text/event-stream").build()
		.execute(new NioCallback() {
			@Override
			public void onConnecting() {

			}

			@Override
			public void onConnected() {

			}

			@Override
			public void onWroteHeaders() {

			}

			@Override
			public void onWroteContentProgressed(long progress, long total) {

			}

			@Override
			public void onWroteContentCompleted() {

			}

			@Override
			public void onReceivedStatus(HttpResponseStatus httpResponseStatus) {

			}

			@Override
			public void onReceivedHeaders(HttpHeaders httpHeaders) {

			}

			@Override
			public void onReceivedContentPart(int len, ByteBuf buffer) {
				System.out.println("Received " + buffer.toString(Charset.defaultCharset()));
			}

			@Override
			public void onReceivedCompleted(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {
				System.out.println("DONE");
			}

			@Override
			public void onError(Throwable throwable) {

			}
		}).get();




	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
