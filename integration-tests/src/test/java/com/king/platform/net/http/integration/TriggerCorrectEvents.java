// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.netty.eventbus.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TriggerCorrectEvents {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		httpClient = testingHttpClientFactory.create();
		recordingEventBus = testingHttpClientFactory.getRecordingEventBus();

		httpClient.start();

	}

	@Test
	public void get200() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write(okBody);
				resp.getWriter().flush();
			}
		}, "/testOk");

		BlockingHttpCallback httpCallback = new BlockingHttpCallback();
		httpClient.createGet("http://localhost:" + port + "/testOk").build().withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(okBody, httpCallback.getBody());
		assertEquals(200, httpCallback.getStatusCode());


		List<Event> expectedEvents = new ArrayList<>();
		expectedEvents.add(Event.onConnecting);
		expectedEvents.add(Event.CREATED_CONNECTION);
		expectedEvents.add(Event.onConnected);
		expectedEvents.add(Event.onAttachedToChannel);
		expectedEvents.add(Event.POPULATE_CONNECTION_SPECIFIC_HEADERS);
		expectedEvents.add(Event.onWroteHeaders);
		expectedEvents.add(Event.onWroteContentCompleted);
		expectedEvents.add(Event.onReceivedStatus);
		expectedEvents.add(Event.onReceivedHeaders);
		expectedEvents.add(Event.onReceivedContentPart);
		expectedEvents.add(Event.onReceivedCompleted);
		expectedEvents.add(Event.onHttpResponseDone);
		expectedEvents.add(Event.COMPLETED);
		expectedEvents.add(Event.CLOSED_CONNECTION);

		List<RecordingEventBus.Interaction> filteredInteractions = recordingEventBus.getFilteredInteractions(RecordingEventBus.InteractionType.TRIGGER, Event
			.TOUCH);

		assertEquals(expectedEvents.size(), filteredInteractions.size());

		Iterator<Event> iterator = expectedEvents.iterator();

		for (RecordingEventBus.Interaction interaction : filteredInteractions) {
			Event expectedEvent = iterator.next();

			assertSame(expectedEvent, interaction.getEvent());

		}

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
