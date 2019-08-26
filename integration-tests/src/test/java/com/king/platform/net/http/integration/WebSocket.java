// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import com.king.platform.net.http.netty.TimeoutException;
import com.king.platform.net.http.netty.backpressure.EvictingBackPressure;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.pool.NoChannelPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketFrameListener;
import org.eclipse.jetty.websocket.api.WebSocketPartialListener;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class WebSocket {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@Before
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer(500);
		integrationServer.start();
		port = integrationServer.getPort();

		recordingEventBus = new RecordingEventBus(new DefaultEventBus());

		httpClient = new NettyHttpClientBuilder()
			.setNioThreads(2)
			.setHttpCallbackExecutorThreads(2)
			.setChannelPool(new NoChannelPool()).setExecutionBackPressure(new EvictingBackPressure(10))
			.setOption(ConfKeys.IDLE_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.NETTY_TRACE_LOGS, true)
			.setRootEventBus(recordingEventBus)
			.createHttpClient();


		httpClient.start();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.sendRedirect("ws://localhost:" + port + "/websocket/test");
			}
		}, "/redirect");

		integrationServer.addServlet(new WebSocketServlet() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.getPolicy().setMaxBinaryMessageBufferSize(1024 * 1024);
				factory.getPolicy().setMaxBinaryMessageSize(1024 * 1024);
				factory.register(EchoWebSocketEndpoint.class);
			}
		}, "/websocket/test");


		integrationServer.addServlet(new WebSocketServlet() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.getPolicy().setMaxBinaryMessageBufferSize(1024 * 1024);
				factory.getPolicy().setMaxBinaryMessageSize(1024 * 1024);
				factory.register(FrameWebSocketEndpoint.class);
			}
		}, "/websocket/frame");


	}

	@Test(timeout = 5000)
	public void webSocket() throws Exception {


		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketListener() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendTextFrame("hello world");
				}

				@Override
				public void onError(Throwable throwable) {
					System.out.println("Client error " + throwable);
				}

				@Override
				public void onDisconnect() {
					countDownLatch.countDown();
				}

				@Override
				public void onCloseFrame(int code, String reason) {

				}

				@Override
				public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

				}

				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedText.set(payload);
					client.sendCloseFrame();
				}
			});


		countDownLatch.await();

		assertEquals("HELLO WORLD", receivedText.get());

	}

	@Test(timeout = 5000)
	public void webSocketRequestEvents() throws Exception {

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketListener() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendTextFrame("hello world");
				}

				@Override
				public void onError(Throwable throwable) {
					System.out.println("Client error " + throwable);
				}

				@Override
				public void onDisconnect() {
					countDownLatch.countDown();

				}

				@Override
				public void onCloseFrame(int code, String reason) {
				}

				@Override
				public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

				}

				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedText.set(payload);
					client.sendCloseFrame();
				}
			});


		countDownLatch.await();
		recordingEventBus.hasTriggered(Event.WS_UPGRADE_PIPELINE);
		recordingEventBus.hasTriggered(Event.COMPLETED);
		recordingEventBus.printDeepInteractionStack();
	}

	@Test(timeout = 5000)
	public void buildAnWebSocketAndLaterConnectIt() throws Exception {


		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		webSocketClient.addListener(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				connection.sendTextFrame("hello world");
			}

			@Override
			public void onError(Throwable throwable) {
				countDownLatch.countDown();
			}

			@Override
			public void onDisconnect() {
				countDownLatch.countDown();
			}

			@Override
			public void onCloseFrame(int code, String reason) {
			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {
				receivedText.set(payload);
				webSocketClient.sendCloseFrame();
			}
		});
		webSocketClient.connect();

		countDownLatch.await();

		assertEquals("HELLO WORLD", receivedText.get());

	}

	@Test
	public void twoConnectDirectlyAfterEachOtherShouldFail() throws Exception {

		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		client.connect();
		try {
			client.connect();
			fail("Should have failed!");
		} catch (Exception ignored) {
		}
	}

	@Test
	public void connectWhenConnectedShouldFail() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		CompletableFuture<WebSocketClient> future = client.connect();

		future.join();

		try {
			client.connect();
			fail("Should have failed!");
		} catch (Exception ignored) {
		}

	}

	@Test
	public void awaitCloseShouldWaitUntilClosed() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		client.addListener(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				connection.sendCloseFrame();
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onDisconnect() {

			}

			@Override
			public void onCloseFrame(int code, String reason) {

			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {

			}
		});

		client.connect().join();

		client.awaitClose();

	}

	@Test(timeout = 5000L)
	public void reconnectShouldWork() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		AtomicInteger connectionCounter = new AtomicInteger();
		AtomicInteger onTextFrameCounter = new AtomicInteger();

		client.addListener(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				client.sendTextFrame("hello world");
				connectionCounter.incrementAndGet();
			}

			@Override
			public void onError(Throwable throwable) {

			}

			@Override
			public void onDisconnect() {

			}

			@Override
			public void onCloseFrame(int code, String reason) {
			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {
				client.sendCloseFrame();
				onTextFrameCounter.incrementAndGet();
			}
		});

		client.connect().join();
		client.awaitClose();

		client.connect().join();
		client.awaitClose();

		assertEquals(2, connectionCounter.get());
		assertEquals(2, onTextFrameCounter.get());


	}

	@Test(timeout = 5000L)
	public void idleTimeout() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.idleTimeoutMillis(500)
			.build()
			.build();

		AtomicReference<Throwable> exceptionReference = new AtomicReference<>();

		client.addListener(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {

			}

			@Override
			public void onError(Throwable throwable) {
				System.out.println("Got error");
				exceptionReference.set(throwable);
			}

			@Override
			public void onDisconnect() {

			}

			@Override
			public void onCloseFrame(int code, String reason) {

			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {

			}
		});

		client.connect().join();
		client.awaitClose();

		Throwable throwable = exceptionReference.get();
		assertNotNull(throwable);

		assertTrue(throwable instanceof TimeoutException);

	}

	@Test(timeout = 5000L)
	public void idleTimeoutShouldNotHappenWhenAutoPingIsEnabled() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.idleTimeoutMillis(500)
			.pingEvery(Duration.of(100, ChronoUnit.MILLIS))
			.build()
			.build();

		CountDownLatch countDownLatch = new CountDownLatch(10);
		AtomicReference<Throwable> exceptionReference = new AtomicReference<>();

		client.addListener(new WebSocketListener() {
			@Override
			public void onConnect(WebSocketConnection connection) {

			}

			@Override
			public void onError(Throwable throwable) {
				exceptionReference.set(throwable);
			}

			@Override
			public void onDisconnect() {

			}

			@Override
			public void onCloseFrame(int code, String reason) {

			}

			@Override
			public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {

			}

			@Override
			public void onPongFrame(byte[] payload) {
				countDownLatch.countDown();
			}
		});

		client.connect().join();

		countDownLatch.await();

		assertNull(exceptionReference.get());


	}

	@Test
	public void endpointRedirects302() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/redirect")
			.followRedirects(true)
			.build()
			.execute(new WebSocketListener() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendTextFrame("hello world");
				}

				@Override
				public void onError(Throwable throwable) {
					System.out.println("Client error " + throwable);
				}

				@Override
				public void onDisconnect() {
					countDownLatch.countDown();
				}

				@Override
				public void onCloseFrame(int code, String reason) {

				}

				@Override
				public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

				}

				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedText.set(payload);
					client.sendCloseFrame();
				}
			});


		countDownLatch.await();

		assertEquals("HELLO WORLD", receivedText.get());
	}

	@Test(timeout = 5000)
	public void webSocketShouldCallOnConnectBeforeItReturns() throws Exception {

		AtomicBoolean onConnect = new AtomicBoolean();

		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketListenerAdapter() {
				@Override
				public void onConnect(WebSocketConnection connection) {
					onConnect.set(true);
				}
			}).get();

		assertTrue(onConnect.get());

		webSocketClient.sendTextFrame("hello");
		webSocketClient.sendCloseFrame();
		webSocketClient.awaitClose();

	}

	@Test
	public void sendingLargeContentShouldSplitIntoFrames() throws ExecutionException, InterruptedException {
		AtomicReference<String> receivedContent = new AtomicReference<>();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketListenerAdapter() {
				@Override
				public void onConnect(WebSocketConnection connection) {

				}

				@Override
				public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
				}

				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedContent.set(payload);
					countDownLatch.countDown();
				}
			}).get();

		byte[] content = new byte[67933];
		new Random().nextBytes(content);

		CompletableFuture<Void> voidCompletableFuture = webSocketClient.sendBinaryFrame(content);
		voidCompletableFuture.join();

		countDownLatch.await(1, TimeUnit.SECONDS);

		assertEquals(Md5Util.getChecksum(content), receivedContent.get());
	}

	@Test
	public void tooLargeFrameShouldTriggerIllegalStateException() {
		byte[] content = new byte[1000];
		try {
			WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
				.maxFrameSize(800)
				.splitLargeFrames(false)
				.build()
				.execute(new WebSocketListenerAdapter() {
				})
				.join();

			client.sendBinaryFrame(content).join();
			fail("Should have thrown exception!");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void sendingPartialBinaryFramesShouldJoinThemInTheServer() throws NoSuchAlgorithmException, InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedMd5 = new AtomicReference<>();

		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.maxFrameSize(1024)
			.splitLargeFrames(false)
			.build()
			.execute(new WebSocketListenerAdapter() {
				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedMd5.set(payload);
				}
			})
			.join();


		MessageDigest md = MessageDigest.getInstance("MD5");
		md.reset();

		Random random = new Random();
		byte[][] frames = new byte[10][];

		for (int i = 0; i < 10; i++) {
			frames[i] = new byte[100];
			random.nextBytes(frames[i]);
			md.update(frames[i], 0, frames[i].length);
		}

		String totalMd5Sum = Md5Util.hexStringFromBytes(md.digest());

		for (int i = 0; i < 9; i++) {
			client.sendBinaryFrame(frames[i], false, 0);
		}
		client.sendBinaryFrame(frames[9], true, 0);

		countDownLatch.await(1, TimeUnit.SECONDS);

		client.close();

		Assert.assertEquals(totalMd5Sum, receivedMd5.get());
	}


	@Test
	public void sendingPartialTextFramesShouldJoinThemInTheServer() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedContent = new AtomicReference<>();

		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.maxFrameSize(1024)
			.splitLargeFrames(false)
			.build()
			.execute(new WebSocketListenerAdapter() {
				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedContent.set(payload);
				}
			})
			.join();



		StringBuilder sentData = new StringBuilder();
		String[] frames = new String[10];

		for (int i = 0; i < 10; i++) {
			frames[i] = "HELLOWORLD"+i;
			sentData.append(frames[i]);
		}


		for (int i = 0; i < 9; i++) {
			client.sendTextFrame(frames[i], false, 0);
		}
		client.sendTextFrame(frames[9], true, 0);

		countDownLatch.await(1, TimeUnit.SECONDS);

		client.close();

		Assert.assertEquals(sentData.toString(), receivedContent.get());
	}

	@Test
	public void sendingPartialTextThenFullTextShouldThrowIllegalState() {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.maxFrameSize(1024)
			.splitLargeFrames(false)
			.build()
			.execute(new WebSocketListenerAdapter() {

			})
			.join();


		client.sendTextFrame("FIRST FRAGMENT", false, 0).join();

		try {
			client.sendTextFrame("FULL NEW TEXT").join();
			fail("Should have thrown exception since its in the middle of sending fragments");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}

		try {
			client.sendBinaryFrame("FULL NEW TEXT".getBytes(StandardCharsets.UTF_8)).join();
			fail("Should have thrown exception since its in the middle of sending fragments");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}

		client.sendTextFrame("SECOND FRAGMENT", false, 0).join();



		client.close();
	}

	@Test
	public void sendingPartialBinaryThenFullTextShouldThrowIllegalState() {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.maxFrameSize(1024)
			.splitLargeFrames(false)
			.build()
			.execute(new WebSocketListenerAdapter() {

			})
			.join();


		client.sendBinaryFrame("FIRST FRAGMENT".getBytes(StandardCharsets.UTF_8), false, 0).join();

		try {
			client.sendTextFrame("FULL NEW TEXT").join();
			fail("Should have thrown exception since its in the middle of sending fragments");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}

		try {
			client.sendBinaryFrame("FULL NEW TEXT".getBytes(StandardCharsets.UTF_8)).join();
			fail("Should have thrown exception since its in the middle of sending fragments");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}

		client.sendBinaryFrame("SECOND FRAGMENT".getBytes(StandardCharsets.UTF_8), false, 0).join();

		client.close();
	}

	@Test
	public void sendingPartialOfDifferentTypesShouldThrowIllegalState() {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.maxFrameSize(1024)
			.splitLargeFrames(false)
			.build()
			.execute(new WebSocketListenerAdapter() {

			})
			.join();


		client.sendBinaryFrame("FIRST FRAGMENT".getBytes(StandardCharsets.UTF_8), false, 0).join();

		try {
			client.sendTextFrame("SECOND TEXT FRAGMENT", false, 0).join();
			fail("Should have thrown exception since its in the middle of sending binary fragments");
		} catch (CompletionException ce) {
			assertTrue(ce.getCause() instanceof IllegalStateException);
		}

	}

	@Test
	public void sendPartial() throws InterruptedException {
		ArrayList<String> receivedData = new ArrayList<>();
		CountDownLatch countDownLatch = new CountDownLatch(10);

		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/frame")
			.maxFrameSize(800)
			.splitLargeFrames(false).build().execute(new WebSocketListenerAdapter() {
				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
					receivedData.add(payload);
					countDownLatch.countDown();
				}
			}).join();

		Random random = new Random();

		byte[][] frames = new byte[10][];
		String[] md5 = new String[10];
		for (int i = 0; i < 10; i++) {
			frames[i] = new byte[100];
			random.nextBytes(frames[i]);
			md5[i] = Md5Util.getChecksum(frames[i]);

		}

		for (int i = 0; i < 9; i++) {
			client.sendBinaryFrame(frames[i], false, 0);
		}
		client.sendBinaryFrame(frames[9], true, 0);


		countDownLatch.await(1, TimeUnit.SECONDS);
		client.close();

		Assert.assertEquals(10, receivedData.size());
		for (int i = 0; i < 9; i++) {
			assertEquals(md5[i] + "/false", receivedData.get(i));
		}
		assertEquals(md5[9] + "/true", receivedData.get(9));

	}


	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}

	public static class EchoWebSocketEndpoint implements org.eclipse.jetty.websocket.api.WebSocketListener {

		private Session session;

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			String checksum = Md5Util.getChecksum(payload);
			try {
				session.getRemote().sendString(checksum);
			} catch (IOException ignored) {
			}
		}

		@Override
		public void onWebSocketText(String message) {
			if ("disconnect".equalsIgnoreCase(message)) {
				try {
					System.out.println("Forcing disconnect of client!");
					session.disconnect();
				} catch (IOException e) {
				}
			} else {
				try {
					session.getRemote().sendString(message.toUpperCase());
				} catch (IOException ignored) {
				}
			}
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
		}

		@Override
		public void onWebSocketConnect(Session session) {
			this.session = session;
		}

		@Override
		public void onWebSocketError(Throwable cause) {
		}
	}

	public static class FrameWebSocketEndpoint implements  WebSocketPartialListener {
		private Session session;



		@Override
		public void onWebSocketClose(int statusCode, String reason) {

		}

		@Override
		public void onWebSocketConnect(Session session) {
			this.session = session;
		}

		@Override
		public void onWebSocketError(Throwable cause) {

		}


		@Override
		public void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteUtil.writeTo(payload, out);
			byte[] bytes = out.toByteArray();
			String output = Md5Util.getChecksum(bytes) + "/" + fin;
			try {
				session.getRemote().sendString(output);
			} catch (IOException ignored) {
			}
		}

		@Override
		public void onWebSocketPartialText(String payload, boolean fin) {

		}



	}


}
