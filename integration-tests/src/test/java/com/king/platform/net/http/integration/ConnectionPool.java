// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import com.king.platform.net.http.netty.pool.PoolingChannelPool;
import com.king.platform.net.http.netty.util.SystemTimeProvider;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ConnectionPool {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private RecordingEventBus rootEventBus;

	@BeforeEach
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();
		rootEventBus = new RecordingEventBus(new DefaultEventBus());

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		httpClient = testingHttpClientFactory.create();


		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");
	}


	private void createHttpClient(boolean useConnectionPool) {
		HashedWheelTimer cleanupTimer = new HashedWheelTimer();
		SystemTimeProvider timeProvider = new SystemTimeProvider();

		ChannelPool pool = new NoChannelPool();
		if (useConnectionPool) {
			pool = new PoolingChannelPool(cleanupTimer, timeProvider,  mock(MetricCallback.class));
		}

		NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2).setRootEventBus(rootEventBus);


		nettyHttpClientBuilder.setChannelPool(pool);


		httpClient = nettyHttpClientBuilder
			.setOption(ConfKeys.IDLE_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS, 0)
			.createHttpClient();


		httpClient.start();
	}


	@Test
	public void getWithNoConnectionPool() throws Exception {
		createHttpClient(false);


		HttpResponse<String> response = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);

		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());

		List<Event> expectedEvents = new ArrayList<>();
		expectedEvents.add(Event.CREATED_CONNECTION);
		expectedEvents.add(Event.COMPLETED);
		expectedEvents.add(Event.CLOSED_CONNECTION);

		validateExpectedEvents(expectedEvents);

	}


	@Test
	public void firstGetWithConnectionPool() throws Exception {
		createHttpClient(true);


		HttpResponse<String> httpResponse = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);

		assertEquals(okBody, httpResponse.getBody());
		assertEquals(200, httpResponse.getStatusCode());


		List<Event> expectedEvents = new ArrayList<>();
		expectedEvents.add(Event.CREATED_CONNECTION);
		expectedEvents.add(Event.COMPLETED);
		expectedEvents.add(Event.POOLED_CONNECTION);

		validateExpectedEvents(expectedEvents);

	}

	@Test
	public void secondGetWithConnectionPool() throws Exception {
		createHttpClient(true);

		HttpResponse<String> response = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);

		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());

		response = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);

		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());


		List<Event> expectedEvents = new ArrayList<>();
		expectedEvents.add(Event.REUSED_CONNECTION);
		expectedEvents.add(Event.COMPLETED);
		expectedEvents.add(Event.POOLED_CONNECTION);

		validateExpectedEvents(expectedEvents);

	}

	@Test
	public void secondGetWithNoConnectionPool() throws Exception {
		createHttpClient(false);


		httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);


		HttpResponse<String> response = httpClient.createGet("http://localhost:" + port + "/testOk").build().execute().get(1, TimeUnit.SECONDS);

		assertEquals(okBody, response.getBody());
		assertEquals(200, response.getStatusCode());


		List<Event> expectedEvents = new ArrayList<>();
		expectedEvents.add(Event.CREATED_CONNECTION);
		expectedEvents.add(Event.COMPLETED);
		expectedEvents.add(Event.CLOSED_CONNECTION);

		validateExpectedEvents(expectedEvents);

	}

	private void validateExpectedEvents(List<Event> expectedEvents) {
		List<RecordingEventBus.Interaction> filteredInteractions = rootEventBus.getFilteredInteractions(RecordingEventBus.InteractionType.TRIGGER);

		for (Event expectedEvent : expectedEvents) {
			boolean foundEvent = false;
			for (RecordingEventBus.Interaction filteredInteraction : filteredInteractions) {
				if (filteredInteraction.getEvent() == expectedEvent) {
					foundEvent = true;
					break;
				}
			}

			if (!foundEvent) {
				fail("Failed to find expected event " + expectedEvent);
			}
		}
	}


	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
