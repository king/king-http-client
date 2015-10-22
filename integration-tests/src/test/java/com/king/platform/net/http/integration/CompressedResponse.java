// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.netty.NettyHttpClient;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CompressedResponse {
	IntegrationServer integrationServer;
	private NettyHttpClient httpClient;
	private int port;

	private String okBody = "Morbi ut pretium augue, eu fringilla tortor. Nulla quis feugiat velit. Proin iaculis justo ut gravida cursus. Fusce nec posuere " +
		"nibh. Donec suscipit orci quis est luctus porta. Cras eget nulla justo. Quisque ut nibh ac ligula fringilla mattis. Donec posuere, urna quis " +
		"condimentum egestas, justo nibh consequat odio, iaculis porta tellus mi vitae ex. Morbi consectetur massa eget mi maximus, ut sodales eros " +
		"vehicula. Aliquam dignissim volutpat massa, a venenatis elit commodo tempor. Aliquam laoreet nisl non mauris lacinia, porttitor euismod tortor " +
		"tristique. Morbi aliquam diam a elementum semper. Cras et tincidunt nulla, eget fermentum diam. Aliquam erat volutpat.";

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		FilterHolder gzipHolder = new FilterHolder(new GzipFilter());
		gzipHolder.setInitParameter("mimeTypes", "text/plain");
		gzipHolder.setInitParameter("minGzipSize", "1");
		gzipHolder.setInitParameter("methods", "GET,POST");

		integrationServer.addFilter(gzipHolder, "/*");

		httpClient = new TestingHttpClientFactory().create();
		httpClient.start();

	}

	@Test
	public void getLocal() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
				resp.setStatus(200);
			}
		}, "/testOk");


		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").acceptCompressedResponse(true).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void get404() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setStatus(404);
			}
		}, "/test404");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/test404").build().execute(httpCallback);
		httpCallback.waitForCompletion();
		assertEquals("", httpCallback.getBody());
		assertEquals(404, httpCallback.getStatusCode());


	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
