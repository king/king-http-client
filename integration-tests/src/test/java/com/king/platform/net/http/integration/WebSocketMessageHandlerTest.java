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
import org.eclipse.jetty.websocket.api.WebSocketPartialListener;
import org.eclipse.jetty.websocket.api.WebSocketPingPongListener;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

import static org.junit.jupiter.api.Assertions.*;

public class WebSocketMessageHandlerTest {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private RecordingEventBus recordingEventBus;

	@BeforeEach
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

		integrationServer.addServlet(new WebSocketServlet() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.register(PingPongWebSocketEndpoint.class);
			}
		}, "/websocket/pingPong");


	}

	@Test
	public void webSocket() throws Exception {


		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendTextMessage("hello world");
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
				public void onTextMessage(String message) {
					receivedText.set(message);
					client.sendCloseFrame();
				}

			});


		countDownLatch.await();

		assertEquals("hello world", receivedText.get());

	}

	@Test
	@org.junit.jupiter.api.Timeout(5000)
	public void webSocketRequestEvents() throws Exception {

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
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
				public void onTextMessage(String message) {
					receivedText.set(message);
					client.sendCloseFrame();
				}

			});


		countDownLatch.await();
		recordingEventBus.hasTriggered(Event.WS_UPGRADE_PIPELINE);
		recordingEventBus.hasTriggered(Event.COMPLETED);
		recordingEventBus.printDeepInteractionStack();
	}

	@Test
	@Timeout(5000)
	public void buildAnWebSocketAndLaterConnectIt() throws Exception {


		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		webSocketClient.addListener(new WebSocketMessageListenerAdapter() {
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
			public void onTextMessage(String message) {
				receivedText.set(message);
				webSocketClient.sendCloseFrame();
			}


		});
		webSocketClient.connect();

		countDownLatch.await();

		assertEquals("hello world", receivedText.get());

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

		client.addListener(new WebSocketMessageListenerAdapter() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				connection.sendCloseFrame();
			}


		});

		client.connect().join();

		client.awaitClose();

	}

	@Test
	@org.junit.jupiter.api.Timeout(5000)
	public void reconnectShouldWork() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.build();

		AtomicInteger connectionCounter = new AtomicInteger();
		AtomicInteger onTextFrameCounter = new AtomicInteger();

		client.addListener(new WebSocketMessageListenerAdapter() {
			@Override
			public void onConnect(WebSocketConnection connection) {
				client.sendTextMessage("hello world");
				connectionCounter.incrementAndGet();
			}


			@Override
			public void onTextMessage(String message) {
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

	@Test
	@org.junit.jupiter.api.Timeout(5000)
	public void idleTimeout() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.idleTimeoutMillis(500)
			.build()
			.build();

		AtomicReference<Throwable> exceptionReference = new AtomicReference<>();

		client.addListener(new WebSocketMessageListenerAdapter() {


			@Override
			public void onError(Throwable throwable) {
				System.out.println("Got error");
				exceptionReference.set(throwable);
			}


		});

		client.connect().join();
		client.awaitClose();

		Throwable throwable = exceptionReference.get();
		assertNotNull(throwable);

		assertTrue(throwable instanceof TimeoutException);

	}

	@Test
	@org.junit.jupiter.api.Timeout(5000)
	public void idleTimeoutShouldNotHappenWhenAutoPingIsEnabled() throws Exception {
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.idleTimeoutMillis(500)
			.pingEvery(Duration.of(100, ChronoUnit.MILLIS))
			.build()
			.build();

		CountDownLatch countDownLatch = new CountDownLatch(10);
		AtomicReference<Throwable> exceptionReference = new AtomicReference<>();

		client.addListener(new WebSocketMessageListenerAdapter() {
			@Override
			public void onError(Throwable throwable) {
				exceptionReference.set(throwable);
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
	public void clientThatSendsPingShouldReceivePong() throws InterruptedException {
		AtomicReference<byte[]> pongRef = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.followRedirects(true)
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendPingFrame("Hello World".getBytes(StandardCharsets.UTF_8));
				}

				@Override
				public void onPongFrame(byte[] payload) {
					pongRef.set(payload);
					client.close();
				}
			}).join().awaitClose();

		assertEquals("Hello World", new String(pongRef.get(), StandardCharsets.UTF_8));
	}

	@Test
	public void whenServerSendsPingTheClientShouldRespondWithAutoPong() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<byte[]> pongResponse = new AtomicReference<>();
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/pingPong")
			.autoPong(true)  //the client will automatically respond to ping frames
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				@Override
				public void onBinaryMessage(byte[] message) {
					pongResponse.set(message);
					countDownLatch.countDown();
				}
			}).join();

		client.sendTextMessage("ping");
		countDownLatch.await();
		client.close();

		assertEquals("SeverSentPing", new String(pongResponse.get(), StandardCharsets.UTF_8));
	}

	@Test
	public void whenServerSendsPingTheClientShouldRespondWithManualPong() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<byte[]> pongResponse = new AtomicReference<>();
		WebSocketClient client = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/pingPong")
			.autoPong(false)  //the client will NOT automatically respond to ping frames
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				private WebSocketConnection connection;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.connection = connection;
				}

				@Override
				public void onBinaryMessage(byte[] message) {
					pongResponse.set(message);
					countDownLatch.countDown();
				}

				@Override
				public void onPingFrame(byte[] payload) {
					connection.sendPongFrame(payload);
				}
			}).join();

		client.sendTextFrame("ping");
		countDownLatch.await();
		client.close();

		assertEquals("SeverSentPing", new String(pongResponse.get(), StandardCharsets.UTF_8));
	}

	@Test
	public void endpointRedirects302() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> receivedText = new AtomicReference<>();

		httpClient.createWebSocket("ws://localhost:" + port + "/redirect")
			.followRedirects(true)
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				WebSocketConnection client;

				@Override
				public void onConnect(WebSocketConnection connection) {
					this.client = connection;
					connection.sendTextMessage("hello world");
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
				public void onTextMessage(String message) {
					receivedText.set(message);
					client.sendCloseFrame();
				}
			});


		countDownLatch.await();

		assertEquals("hello world", receivedText.get());
	}

	@Test
	@org.junit.jupiter.api.Timeout(5000)
	public void webSocketShouldCallOnConnectBeforeItReturns() throws Exception {

		AtomicBoolean onConnect = new AtomicBoolean();

		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				@Override
				public void onConnect(WebSocketConnection connection) {
					onConnect.set(true);
				}
			}).get();

		assertTrue(onConnect.get());

		webSocketClient.sendTextMessage("hello");
		webSocketClient.sendCloseFrame();
		webSocketClient.awaitClose();

	}

	@Test
	public void sendingLargeContentShouldSplitIntoFrames() throws ExecutionException, InterruptedException {
		AtomicReference<String> receivedContent = new AtomicReference<>();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				@Override
				public void onTextMessage(String message) {
					receivedContent.set(message);
					countDownLatch.countDown();
				}
			}).get();

		byte[] content = new byte[128000];
		new Random().nextBytes(content);

		webSocketClient.sendBinaryMessage(content).join();

		countDownLatch.await(1, TimeUnit.SECONDS);

		assertEquals(Md5Util.getChecksum(content), receivedContent.get());
	}

	@Test
	public void sendingLargetTextContetShouldSplitIntoFrames() throws ExecutionException, InterruptedException {
		AtomicReference<String> receivedContent = new AtomicReference<>();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.build()
			.execute(new WebSocketMessageListenerAdapter() {
				@Override
				public void onTextMessage(String message) {
					receivedContent.set(message);
					countDownLatch.countDown();
				}
			}).get();

		webSocketClient.sendTextMessage(getUtf8Text()).join();

		countDownLatch.await(1, TimeUnit.SECONDS);

		assertEquals(getUtf8Text(), receivedContent.get());
	}



	@Test
	void clientSendingCloseShouldCloseConnectionAfterReceivingCloseResponseFromServer() throws InterruptedException {
		AtomicReference<String> reasonRef = new AtomicReference<>();
		AtomicInteger codeRef = new AtomicInteger();
		WebSocketClient webSocketClient = httpClient.createWebSocket("ws://localhost:" + port + "/websocket/test")
			.autoCloseFrame(false)
			.build().execute(new WebSocketListenerAdapter() {
				@Override
				public void onCloseFrame(int code, String reason) {
					codeRef.set(code);
					reasonRef.set(reason);
				}
			}).join();


		webSocketClient.sendCloseFrame(1002, "client-closing");

		webSocketClient.awaitClose();

		assertEquals("client-closing", reasonRef.get());
		assertEquals(1002, codeRef.get());

	}

	@AfterEach
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
					session.getRemote().sendString(message);
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

	public static class FrameWebSocketEndpoint implements WebSocketPartialListener {
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

	public static class PingPongWebSocketEndpoint implements WebSocketPingPongListener, org.eclipse.jetty.websocket.api.WebSocketListener {
		private Session session;

		@Override
		public void onWebSocketPing(ByteBuffer payload) {

		}

		@Override
		public void onWebSocketPong(ByteBuffer payload) {
			try {
				session.getRemote().sendBytes(payload);
			} catch (IOException e) {
				session.close();
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

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {

		}

		@Override
		public void onWebSocketText(String message) {
			if (message.equalsIgnoreCase("ping")) {
				try {
					session.getRemote().sendPing(ByteBuffer.wrap("SeverSentPing".getBytes(StandardCharsets.UTF_8)));
				} catch (IOException e) {
					session.close();
				}
			}
		}
	}

	private static String getUtf8Text() {
		return "Icelandic (is)\n" +
			"--------------\n" +
			"\n" +
			"  Kæmi ný öxi hér ykist þjófum nú bæði víl og ádrepa\n" +
			"\n" +
			"  Sævör grét áðan því úlpan var ónýt\n" +
			"  (some ASCII letters missing)\n" +
			"\n" +
			"Japanese (jp)\n" +
			"-------------\n" +
			"\n" +
			"  Hiragana: (Iroha)\n" +
			"\n" +
			"  いろはにほへとちりぬるを\n" +
			"  わかよたれそつねならむ\n" +
			"  うゐのおくやまけふこえて\n" +
			"  あさきゆめみしゑひもせす\n" +
			"\n" +
			"  Katakana:\n" +
			"\n" +
			"  イロハニホヘト チリヌルヲ ワカヨタレソ ツネナラム\n" +
			"  ウヰノオクヤマ ケフコエテ アサキユメミシ ヱヒモセスン\n" +
			"\n" +
			"Hebrew (iw)\n" +
			"-----------\n" +
			"\n" +
			"  ? דג סקרן שט בים מאוכזב ולפתע מצא לו חברה איך הקליטה";
	}


}
