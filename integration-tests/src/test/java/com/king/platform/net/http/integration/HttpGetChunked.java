// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class HttpGetChunked {

	private static final String message1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
	private static final String message2 = "Aenean ut leo elit.";
	private static final String message3 = "Aenean rutrum tincidunt arcu, lacinia scelerisque.";

	private int port;
	private HttpClient httpClient;
	private Server server;

	@Before
	public void setUp() throws Exception {
		port = JettyIntegrationServer.findFreePort();

		server = new Server(port);
		server.setHandler(new MyHandler());
		server.getBean(ServerConnector.class).setIdleTimeout(2000);

		server.start();

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();
	}

	@Test
	public void getChunked() throws Exception {
		HttpResponse<String> httpResponse = httpClient.createGet("http://localhost:" + port).keepAlive(true).build().execute().get();
		String body = httpResponse.getBody();
		assertEquals(message1 +"\r\n"+ message2 +"\r\n"+ message3 +"\r\n", body);
		String header = httpResponse.getHeader("Transfer-Encoding");
		assertEquals("chunked", header);
	}


	@After
	public void tearDown() throws Exception {
		httpClient.shutdown();
		server.stop();
	}

	public static class MyHandler extends AbstractHandler {
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			baseRequest.setHandled(true);
			response.setContentType("text/plain; charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);

			ServletOutputStream out = response.getOutputStream();

			out.println(message1);
			out.flush();
			out.println(message2);
			out.flush();
			out.println(message3);
		}
	}
}
