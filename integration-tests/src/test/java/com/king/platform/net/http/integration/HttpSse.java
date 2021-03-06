// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.EventCallback;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.SseClient;
import com.king.platform.net.http.SseClientCallback;
import com.king.platform.net.http.netty.TimeoutException;
import com.king.platform.net.http.netty.eventbus.Event;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class HttpSse {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@BeforeEach
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

		final AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseClientCallback() {
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
	public void getSSEThatReturns500() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.sendError(500, "Internal Error");
			}
		}, "/test500");


		final AtomicReference<String> errorMessage = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/test500").build().execute(new SseClientCallback() {
			@Override
			public void onConnect() {
			}

			@Override
			public void onDisconnect() {
			}

			@Override
			public void onError(Throwable throwable) {
				errorMessage.set(throwable.getMessage());
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {

			}
		});

		sseClient.awaitClose(); //block until complete
		assertTrue(errorMessage.get().contains("Internal Error"));

	}

	@Test
	public void getSSEWithCustomExecutor() throws Exception {
		integrationServer.addServlet(new CountingEventSourceServlet(), "/sse");


		final CountDownLatch countDownLatch = new CountDownLatch(4);
		final Set<String> threadNames = new HashSet<>();

		httpClient.createSSE("http://localhost:" + port + "/sse")
			.executingOn(Executors.newFixedThreadPool(1)).build().execute(new SseClientCallback() {

			@Override
			public void onConnect() {
				threadNames.add(Thread.currentThread().getName());
			}

			@Override
			public void onDisconnect() {
				threadNames.add(Thread.currentThread().getName());

			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				threadNames.add(Thread.currentThread().getName());
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {
				}
				countDownLatch.countDown();
			}
		});

		countDownLatch.await();

		assertEquals(1, threadNames.size());

	}

	@Test
	public void getSSeThatRedirects() throws Exception {
		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.sendRedirect("/sse2");
			}
		}, "/sse1");

		integrationServer.addServlet(new CountingEventSourceServlet(), "/sse2");

		final CountDownLatch countDownLatch = new CountDownLatch(2);
		final AtomicInteger counter = new AtomicInteger();
		final AtomicBoolean onError = new AtomicBoolean();
		final AtomicInteger onConnectCounter = new AtomicInteger();
		final AtomicInteger onDisconnectCounter = new AtomicInteger();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/sse1").followRedirects(true).build().execute(new SseClientCallback() {

			@Override
			public void onConnect() {
				onConnectCounter.incrementAndGet();
			}

			@Override
			public void onDisconnect() {
				onDisconnectCounter.incrementAndGet();
			}

			@Override
			public void onError(Throwable throwable) {
				throwable.printStackTrace();
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
		assertEquals(1, onConnectCounter.get());
		assertEquals(1, onDisconnectCounter.get());
		assertFalse(onError.get());

	}

	@Test
	public void getSseAndClose() throws Exception {
		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");

		final CountDownLatch countDownLatch = new CountDownLatch(2);
		final AtomicInteger counter = new AtomicInteger();
		final AtomicBoolean onError = new AtomicBoolean();
		final AtomicBoolean onConnect = new AtomicBoolean();
		final AtomicBoolean onDisconnect = new AtomicBoolean();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseClientCallback() {

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

		final AtomicReference<String> output = new AtomicReference<>();

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().execute(new SseClientCallback() {
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

		sseClient.connect();

		sseClient.awaitClose();
		assertEquals("01234567890123456789", output.get());

	}

	@Test
	public void sse404() throws Exception {
		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/noSSE").build().build();

		final AtomicBoolean exception = new AtomicBoolean(false);
		final AtomicBoolean connected = new AtomicBoolean(false);
		final AtomicBoolean disconnected = new AtomicBoolean(false);

		sseClient.addCallback(new SseClientCallback() {
			@Override
			public void onConnect() {
				connected.set(true);
			}

			@Override
			public void onDisconnect() {
				disconnected.set(true);
			}

			@Override
			public void onError(Throwable throwable) {
				exception.set(true);
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {

			}
		});



		sseClient.connect();
		sseClient.awaitClose();

		assertTrue(exception.get());
		assertFalse(connected.get());
		assertFalse(disconnected.get());
	}

	@Test
	public void sseNoServer() throws Exception {
		SseClient sseClient = httpClient.createSSE("http://no-server:" + port + "/noSSE").build().build();

		final AtomicBoolean exception = new AtomicBoolean(false);
		final AtomicBoolean connected = new AtomicBoolean(false);
		final AtomicBoolean disconnected = new AtomicBoolean(false);

		sseClient.addCallback(new SseClientCallback() {
			@Override
			public void onConnect() {
				connected.set(true);
			}

			@Override
			public void onDisconnect() {
				disconnected.set(true);
			}

			@Override
			public void onError(Throwable throwable) {
				exception.set(true);
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {

			}
		});

		final AtomicBoolean exceptionCallback = new AtomicBoolean();
		sseClient.onError(new SseClient.ErrorCallback() {
			@Override
			public void onError(Throwable throwable) {
				exceptionCallback.set(true);
			}
		});

		sseClient.connect();
		sseClient.awaitClose();

		assertTrue(exception.get());
		assertTrue(exceptionCallback.get());
		assertFalse(connected.get());
		assertFalse(disconnected.get());
	}


	@Test
	public void addCallbackToClient() throws Exception {
		integrationServer.addServlet(new CountingEventSourceServlet(), "/testSSE");


		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").build().build();
		final AtomicInteger connected = new AtomicInteger();
		final AtomicInteger disconnected = new AtomicInteger();
		final AtomicInteger events = new AtomicInteger();

		final CountDownLatch countDownLatch = new CountDownLatch(3);

		SseClientCallback callback = new SseClientCallback() {
			@Override
			public void onConnect() {
				connected.incrementAndGet();
			}

			@Override
			public void onDisconnect() {
				disconnected.incrementAndGet();
			}

			@Override
			public void onError(Throwable throwable) {
				fail(throwable.getMessage());
			}

			@Override
			public void onEvent(String lastSentId, String event, String data) {
				events.incrementAndGet();
			}
		};

		sseClient.addCallback(callback);
		sseClient.addCallback(callback);
		sseClient.addCallback(callback);

		sseClient.onEvent(new EventCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				countDownLatch.countDown();
			}
		});

		final AtomicBoolean connectedCallback = new AtomicBoolean();
		sseClient.onConnect(new SseClient.ConnectCallback() {
			@Override
			public void onConnect() {
				connectedCallback.set(true);
			}
		});

		final AtomicBoolean disconnectedCallback = new AtomicBoolean();
		sseClient.onDisconnect(new SseClient.DisconnectCallback() {
			@Override
			public void onDisconnect() {
				disconnectedCallback.set(true);
			}
		});

		sseClient.connect();

		countDownLatch.await();

		sseClient.close();

		sseClient.awaitClose();

		assertEquals(3, connected.get());
		assertEquals(3, disconnected.get());
		assertEquals(9, events.get());

		assertTrue(connectedCallback.get());
		assertTrue(disconnectedCallback.get());

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


		final List<EventData> receivedEvents = new ArrayList<>();

		EventCallback callback = new EventCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				receivedEvents.add(new EventData(event, data));
			}
		};

		sseClient.onEvent("event1", callback);
		sseClient.onEvent("event2", callback);
		sseClient.onEvent("event3", callback);

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

		final List<EventData> receivedEvents1 = new ArrayList<>();
		sseClient.onEvent("event1", new EventCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				receivedEvents1.add(new EventData(event, data));

			}
		});


		final List<EventData> receivedEvents2 = new ArrayList<>();
		sseClient.onEvent("event1", new EventCallback() {
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

		sseClient.onEvent(new EventCallback() {
			@Override
			public void onEvent(String lastSentId, String event, String data) {
				buffer.set(buffer.get() + data);

			}
		});

		sseClient.awaitClose(); //block until complete

		assertEquals("0123456789", buffer.get());
	}


	@Test
	public void idleTimeOutShouldNotTriggerTooEarly() throws InterruptedException {
		List<EventData> events = new ArrayList<>();
		events.add(new EventData("event1", "data1", 500));
		events.add(new EventData("event1", "data2", 2000));
		events.add(new EventData("event1", "data3", 1500));

		integrationServer.addServlet(new EmittingEventSourceServlet(events), "/testSSE");

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").idleTimeoutMillis(3000).build().execute();

		final List<EventData> receivedEvents = new ArrayList<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		sseClient.onEvent("event1", (lastSentId, event, data) -> receivedEvents.add(new EventData(event, data)));
		sseClient.onError(error::set);
		sseClient.awaitClose();

		assertNull(error.get());

		assertEquals(3, receivedEvents.size());

	}

	@Test
	public void idleTimeOutShouldTrigger() throws InterruptedException {
		List<EventData> events = new ArrayList<>();
		events.add(new EventData("event1", "data1", 500));
		events.add(new EventData("event1", "data2", 2000));
		events.add(new EventData("event1", "data3", 1500));

		integrationServer.addServlet(new EmittingEventSourceServlet(events), "/testSSE");

		SseClient sseClient = httpClient.createSSE("http://localhost:" + port + "/testSSE").idleTimeoutMillis(700).build().execute();

		final List<EventData> receivedEvents = new ArrayList<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		sseClient.onEvent("event1", (lastSentId, event, data) -> receivedEvents.add(new EventData(event, data)));
		sseClient.onError(error::set);
		sseClient.awaitClose();

		assertEquals(2, receivedEvents.size());
		assertNotNull(error.get());
		assertTrue(error.get() instanceof TimeoutException);
	}


	@AfterEach
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
									Thread.sleep(50);
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
				public void onOpen(final Emitter emitter) throws IOException {
					new Thread(new Runnable() {
						@Override
						public void run() {

							for (EventData event : events) {
								try {
									emitter.event(event.name, event.data);
									Thread.sleep(event.sleepTime);
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

	private final static class EventData {
		String name;
		String data;
		int sleepTime = 50;

		public EventData(String name, String data) {
			this.name = name;
			this.data = data;
		}

		public EventData(String name, String data, int sleepTime) {
			this.name = name;
			this.data = data;
			this.sleepTime = sleepTime;
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
