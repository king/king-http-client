// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.netty.NettyHttpClient;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;

public class KeepAlive {
	IntegrationServer integrationServer;
	private NettyHttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		recordingEventBus = new RecordingEventBus(new DefaultEventBus());

		NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder().setNioThreads(2).setHttpCallbackExecutorThreads(2).setRootEventBus
			(recordingEventBus);

		httpClient = nettyHttpClientBuilder.createHttpClient();

		httpClient.start();

	}

	@Test
	public void connectionShouldCloseIfServerClosesIt() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setHeader("connection", "close");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertTrue(recordingEventBus.hasTriggered(Event.CLOSED_CONNECTION));
		assertFalse(recordingEventBus.hasTriggered(Event.POOLED_CONNECTION));


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@Test
	public void connectionShouldCloseIfServerClosesItEvenIfItsKeepAlive() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setHeader("connection", "close");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").keepAlive(true).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertTrue(recordingEventBus.hasTriggered(Event.CLOSED_CONNECTION));
		assertFalse(recordingEventBus.hasTriggered(Event.POOLED_CONNECTION));


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}


	@Test
	public void connectionShouldPoolIfKeepAliveAndServerAllowsIt() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setHeader("connection", "keep-alive");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").keepAlive(true).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertFalse(recordingEventBus.hasTriggered(Event.CLOSED_CONNECTION));
		assertTrue(recordingEventBus.hasTriggered(Event.POOLED_CONNECTION));


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}


	@Test
	public void connectionShouldNotPoolIfNoitKeepAliveAndServerAllowsIt() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setHeader("connection", "keep-alive");
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").keepAlive(false).build().execute(httpCallback);
		httpCallback.waitForCompletion();

		assertTrue(recordingEventBus.hasTriggered(Event.CLOSED_CONNECTION));
		assertFalse(recordingEventBus.hasTriggered(Event.POOLED_CONNECTION));


		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
