// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class HttpGetAndServerClose {
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@Before
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
					outputStream.write("\r\n".getBytes(Charset.defaultCharset()));
					outputStream.write("Hello World\r\n".getBytes(Charset.defaultCharset()));
					outputStream.flush();
					outputStream.close();
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/").build().execute(httpCallback);
		httpCallback.waitForCompletion();
		recordingEventBus.printDeepInteractionStack();
		assertEquals("OK", httpCallback.getHeader("X-Status"));
		assertEquals(200, httpCallback.getStatusCode());

	}


	@After
	public void tearDown() throws Exception {

		httpClient.shutdown();

	}

}
