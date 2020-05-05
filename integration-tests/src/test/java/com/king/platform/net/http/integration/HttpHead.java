// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

public class HttpHead {
	private final Logger logger = getLogger(getClass());

	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okHeader = "EVERYTHING IS OKAY!";
	private RecordingEventBus recordingEventBus;

	@BeforeEach
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(5000);
		integrationServer.start();
		port = integrationServer.getPort();

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		recordingEventBus = testingHttpClientFactory.getRecordingEventBus();
		httpClient = testingHttpClientFactory.useChannelPool().create();
		httpClient.start();

	}


	@Test
	public void head200WithClose() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.addHeader("X-OK", okHeader);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void head200() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("X-OK", okHeader);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("", httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void head200WithContentLength() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.addHeader("Connection", "close");
				resp.addHeader("X-OK", okHeader);
				resp.setContentLength(512);
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals("", httpCallback.getBody());
		assertEquals(okHeader, httpCallback.getHeader("X-OK"));
		assertEquals("512", httpCallback.getHeader("content-length"));
		assertEquals(200, httpCallback.getStatusCode());

	}

	@Test
	public void get404() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setStatus(404);
			}
		}, "/test404");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createHead("http://localhost:" + port + "/test404").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());


	}

	@Test
	public void headWithFaultyServerThatWritesBody() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		int port = JettyIntegrationServer.findFreePort();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					Socket clientSocket = serverSocket.accept();

					OutputStream outputStream = clientSocket.getOutputStream();

					outputStream.write("HTTP/1.0 200 OK\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Server: JavaSocket\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("X-Status: OK\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Content-Length: 13\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Hello World\r\n".getBytes(Charset.defaultCharset()));
					outputStream.flush();
				} catch (IOException e) {
					logger.error("Failed to write to socket", e);
				}
			}
		}).start();

		HttpResponse<String> headResponse = httpClient.createHead("http://localhost:" + port + "/")
			.keepAlive(true)
			.idleTimeoutMillis(1000)
			.build()
			.execute()
			.get(2000, TimeUnit.MILLISECONDS);
		assertEquals(headResponse.getStatusCode(), 200);
		assertEquals("", headResponse.getBody());

		Thread.sleep(100);

		assertEquals(1, recordingEventBus.getTriggeredCount(Event.COMPLETED));
		assertEquals(0, recordingEventBus.getTriggeredCount(Event.ERROR));

		assertEquals(1, recordingEventBus.getTriggeredCount(Event.onReceivedStatus));
		assertEquals(1, recordingEventBus.getTriggeredCount(Event.onReceivedHeaders));


	}

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
