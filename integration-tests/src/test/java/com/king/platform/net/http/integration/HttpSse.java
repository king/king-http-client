// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.eventbus.Event;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HttpSse {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(5000);
		integrationServer.start();
		port = integrationServer.getPort();

		TestingHttpClientFactory testingHttpClientFactory = new TestingHttpClientFactory();
		recordingEventBus = testingHttpClientFactory.getRecordingEventBus();
		httpClient = testingHttpClientFactory.create();
		httpClient.start();

	}

	@Test
	public void getSSE() throws Exception {

		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");

		AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseExecutionCallback() {
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



	@Test
	public void getSseAndClose() throws Exception {
		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");

		CountDownLatch countDownLatch = new CountDownLatch(2);
		AtomicInteger counter = new AtomicInteger();
		AtomicBoolean onError = new AtomicBoolean();
		AtomicBoolean onConnect = new AtomicBoolean();
		AtomicBoolean onDisconnect = new AtomicBoolean();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseExecutionCallback() {

			@Override
			public void onConnect() {
				onConnect.set(true);
			}

			@Override
			public void onDisconnect() {
				onDisconnect.set(true);
			}

			@Override
			public void onError(Throwable throwable) {
				onError.set(true);
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				counter.incrementAndGet();
				countDownLatch.countDown();

			}
		});

		countDownLatch.await();

		sseClient.close();
		Thread.sleep(400);

		assertEquals(2, counter.get());

		assertTrue(onConnect.get());
		assertTrue(onDisconnect.get());
		assertFalse(onError.get());

		assertEquals(1, recordingEventBus.getTriggeredCount(Event.COMPLETED));

	}

	@Test
	public void reconnect() throws Exception {
		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");

		AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseExecutionCallback() {
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

		sseClient.awaitClose();

		assertEquals("0123456789", output.get());

		sseClient.reconnect();

		sseClient.awaitClose();
		assertEquals("01234567890123456789", output.get());

	}

	@Test
	public void addEventTypeSubscriptions() throws Exception {

		List<EventData> events = new ArrayList<>();
		events.add(new EventData("event1", "data1"));
		events.add(new EventData("event2", "data2"));
		events.add(new EventData("event3", "data3"));
		events.add(new EventData("event4", "data4"));
		events.add(new EventData("event5", "data5"));

		integrationServer.addServlet(new EmittingEventSourceServlet(events), "/testSSE");

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute();


		List<EventData> receivedEvents = new ArrayList<>();

		SseCallback callback = new SseCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				receivedEvents.add(new EventData(event, data));
			}
		};

		sseClient.subscribe("event1", callback);
		sseClient.subscribe("event2", callback);
		sseClient.subscribe("event3", callback);

		sseClient.awaitClose();

		assertEquals(3, receivedEvents.size());

		for (EventData receivedEvent : receivedEvents) {
			assertTrue(events.contains(receivedEvent));
		}
	}

	@Test
	public void addMultipleOfTheSameEventCallbacks() throws Exception {
		List<EventData> events = new ArrayList<>();
		events.add(new EventData("event1", "data1"));
		events.add(new EventData("event1", "data2"));
		events.add(new EventData("event1", "data3"));


		integrationServer.addServlet(new EmittingEventSourceServlet(events), "/testSSE");

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute();

		List<EventData> receivedEvents1 = new ArrayList<>();
		sseClient.subscribe("event1", new SseCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				receivedEvents1.add(new EventData(event, data));

			}
		});


		List<EventData> receivedEvents2 = new ArrayList<>();
		sseClient.subscribe("event1", new SseCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				receivedEvents2.add(new EventData(event, data));

			}
		});

		sseClient.awaitClose();


		assertEquals(3, receivedEvents1.size());
		assertEquals(3, receivedEvents2.size());

	}

	@Test
	public void addGenericCallback() throws Exception {

		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");


		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute();
		final AtomicReference<String> buffer = new AtomicReference<>("");

		sseClient.subscribe(new SseCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				buffer.set(buffer.get() + data);

			}
		});

		sseClient.awaitClose(); //block until complete

		assertEquals("0123456789", buffer.get());
	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


	private static class CountingEventSourceServlet extends EventSourceServlet {

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
									Thread.sleep(100);
									emitter.data("" + i);
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
	}

	private static class EmittingEventSourceServlet extends EventSourceServlet {
		private final List<EventData> events;

		private EmittingEventSourceServlet(List<EventData> events) {
			this.events = events;
		}

		@Override
		protected EventSource newEventSource(HttpServletRequest request) {
			return new EventSource() {
				@Override
				public void onOpen(Emitter emitter) throws IOException {
					new Thread(new Runnable() {
						@Override
						public void run() {

							for (EventData event : events) {
								try {
									emitter.event(event.name, event.data);
									Thread.sleep(100);
								} catch (IOException e) {
									return;
								} catch (InterruptedException ignored) {
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
	}

	private static class EventData {
		String name;
		String data;

		public EventData(String name, String data) {
			this.name = name;
			this.data = data;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			EventData eventData = (EventData) o;

			if (name != null ? !name.equals(eventData.name) : eventData.name != null)
				return false;
			return data != null ? data.equals(eventData.data) : eventData.data == null;

		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (data != null ? data.hashCode() : 0);
			return result;
		}
	}

}
