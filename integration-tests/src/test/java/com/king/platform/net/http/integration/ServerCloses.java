// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.ConnectionClosedException;
import com.king.platform.net.http.netty.eventbus.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

public class ServerCloses {
	private final Logger logger = getLogger(getClass());

	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@BeforeEach
	public void setUp() throws Exception {

		port = JettyIntegrationServer.findFreePort();

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		httpClient = testingHttpClientFactory.create();
		httpClient.start();

		recordingEventBus = testingHttpClientFactory.getRecordingEventBus();

	}


	@Test
	public void get200() throws Exception {


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
					outputStream.close();
					clientSocket.close();
				} catch (IOException e) {
					logger.error("Failed to write to socket", e);
				}
			}
		}).start();


		HttpResponse<String> response = httpClient.createGet("http://localhost:" + port + "/").build().execute().get(1, TimeUnit.SECONDS);
		assertEquals("OK", response.getHeader("X-Status"));
		assertEquals(200, response.getStatusCode());

		assertFalse(recordingEventBus.hasTriggered(Event.ERROR));
		assertTrue(recordingEventBus.hasTriggered(Event.COMPLETED));
	}

	@Test
	public void serverSocketReset() throws Exception {

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
					outputStream.write("\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Hello World\r\n".getBytes(Charset.defaultCharset()));
					outputStream.flush();
					outputStream.flush();
					outputStream.flush();
					try {
						Thread.sleep(200);
					} catch (InterruptedException ignored) {

					}

					clientSocket.setSoLinger(true, 0); //forces TCP RST package instead of FIN
					clientSocket.close();
				} catch (IOException e) {
					logger.error("Failed to write to socket", e);
				}
			}
		}).start();


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/").idleTimeoutMillis(50000).build().withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();
		Thread.sleep(5);

		assertNotNull(httpCallback.getException());
		assertTrue(httpCallback.getException().getMessage().contains("reset"), "Exception was: " + httpCallback.getException().getMessage());

		assertFalse(recordingEventBus.hasTriggered(Event.COMPLETED));
		assertTrue(recordingEventBus.hasTriggered(Event.ERROR));

	}

	@Test
	public void serverClosesBeforeAllContentIsSent() throws Exception {

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
					outputStream.write("Content-Length: 1024\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam id lobortis lacus, nec ultrices augue.\r\n".getBytes
						(Charset
						.defaultCharset()));
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					logger.error("Failed to write to socket", e);
				}
			}
		}).start();


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();
		Thread.sleep(5);

		assertNotNull(httpCallback.getException());
		assertTrue(httpCallback.getException() instanceof ConnectionClosedException);


	}

	@Test
	public void serverClosesWhileClientSends() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					Socket clientSocket = serverSocket.accept();
					InputStream inputStream = clientSocket.getInputStream();
					inputStream.read(new byte[10]);
					OutputStream outputStream = clientSocket.getOutputStream();
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					logger.error("Failed to write to socket", e);
				}
			}

		}).start();


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();

		httpClient.createPost("http://localhost:" + port + "/")
			.content("some data that needs to be sent to server".getBytes(StandardCharsets.UTF_8))
			.build()
			.withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();
		Thread.sleep(5);

		assertNotNull(httpCallback.getException());
		assertTrue(httpCallback.getException() instanceof IOException);

	}

	@AfterEach
	public void tearDown() throws Exception {

		httpClient.shutdown();

	}

}
