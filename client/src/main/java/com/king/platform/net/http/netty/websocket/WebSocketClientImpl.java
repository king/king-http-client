package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.Headers;
import com.king.platform.net.http.WebSocketClient;
import com.king.platform.net.http.WebSocketListener;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import com.king.platform.net.http.netty.util.AwaitLatch;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class WebSocketClientImpl implements WebSocketClient {
	private final Logger logger = getLogger(getClass());

	private final CharsetDecoder utf8Decoder = Charset.forName("UTF8").newDecoder().onMalformedInput(CodingErrorAction.REPORT);

	private final CopyOnWriteArrayList<WebSocketListener> listeners = new CopyOnWriteArrayList<>();
	private final BuiltNettyClientRequest<Void> builtNettyClientRequest;
	private final Executor callbackExecutor;
	private final Executor completableFutureExecutor;
	private final boolean autoPong;
	private final boolean autoCloseFrame;
	private final Duration pingEveryDuration;

	private Headers headers = new Headers(EmptyHttpHeaders.INSTANCE);

	private FragmentedFrameType expectedFragmentedFrameType;

	private List<WebSocketFrame> bufferedFrames = new ArrayList<>();

	private volatile Channel channel;
	private volatile boolean ready;
	private volatile CompletableFuture<WebSocketClient> connectionFuture;
	private final AwaitLatch awaitLatch = new AwaitLatch();
	private ScheduledFuture<?> pingFuture;


	public WebSocketClientImpl(BuiltNettyClientRequest<Void> builtNettyClientRequest, Executor listenerExecutor, Executor completableFutureExecutor, boolean
		autoPong, boolean autoCloseFrame, Duration pingEveryDuration) {
		this.builtNettyClientRequest = builtNettyClientRequest;
		this.callbackExecutor = listenerExecutor;
		this.completableFutureExecutor = completableFutureExecutor;
		this.autoPong = autoPong;
		this.autoCloseFrame = autoCloseFrame;
		this.pingEveryDuration = pingEveryDuration;


		builtNettyClientRequest.withCustomCallbackSupplier(requestEventBus -> {
			requestEventBus.subscribe(Event.onWsOpen, WebSocketClientImpl.this::onOpen);
			requestEventBus.subscribe(Event.onWsFrame, WebSocketClientImpl.this::onWebSocketFrame);
			requestEventBus.subscribe(Event.ERROR, WebSocketClientImpl.this::onError);
			requestEventBus.subscribe(Event.COMPLETED, WebSocketClientImpl.this::onCompleted);
		});

	}

	@Override
	public Headers headers() {
		return headers;
	}

	@Override
	public String getNegotiatedSubProtocol() {
		return headers.get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);

	}

	@Override
	public void addListener(WebSocketListener webSocketListener) {
		listeners.add(webSocketListener);
	}

	@Override
	public CompletableFuture<WebSocketClient> connect() {
		if (connectionFuture != null) {
			throw new IllegalStateException("Already trying to connect!");
		}

		if (!ready) {
			connectionFuture = new CompletableFuture<>();
			builtNettyClientRequest.execute();
			return connectionFuture;
		} else {
			throw new IllegalStateException("Already connected");
		}

	}

	@Override
	public void awaitClose() throws InterruptedException {
		if (connectionFuture == null && channel == null) {
			return;
		}

		awaitLatch.awaitClose();
	}

	@Override
	public boolean isConnected() {
		return ready;
	}

	@Override
	public CompletableFuture<Void> sendTextFrame(String text) {
		if (!ready || channel == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}
		return convert(channel.writeAndFlush(new TextWebSocketFrame(text)));
	}

	@Override
	public CompletableFuture<Void> sendCloseFrame(int statusCode, String reason) {
		if (!ready || channel == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		if (channel.isOpen()) {
			channel.writeAndFlush(new CloseWebSocketFrame(statusCode, reason));
		}

		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> sendCloseFrame() {
		return sendCloseFrame(1000, "");
	}

	@Override
	public CompletableFuture<Void> sendBinaryFrame(byte[] payload) {
		if (!ready || channel == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload))));
	}

	@Override
	public CompletableFuture<Void> sendPingFrame() {
		if (!ready || channel == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PingWebSocketFrame()));
	}

	@Override
	public CompletableFuture<Void> sendPingFrame(byte[] payload) {
		if (!ready || channel == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer(payload))));
	}


	private CompletableFuture<Void> convert(ChannelFuture f) {
		CompletableFuture<Void> completableFuture = new CompletableFuture<>();
		f.addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				completableFuture.complete(null);
			} else {
				completableFuture.completeExceptionally(future.cause());
			}
		});
		return completableFuture;
	}

	private void onWebSocketFrame(WebSocketFrame frame) {
		if (!ready) {
			bufferedFrames.add(frame);
			frame.retain();
		} else {
			if (frame instanceof TextWebSocketFrame) {
				onTextFrame((TextWebSocketFrame) frame);
			} else if (frame instanceof BinaryWebSocketFrame) {
				onBinaryFrame((BinaryWebSocketFrame) frame);
			} else if (frame instanceof CloseWebSocketFrame) {
				onClose((CloseWebSocketFrame) frame);
			} else if (frame instanceof PingWebSocketFrame) {
				onPingFrame((PingWebSocketFrame) frame);
			} else if (frame instanceof PongWebSocketFrame) {
				onPongFrame((PongWebSocketFrame) frame);
			} else if (frame instanceof ContinuationWebSocketFrame) {
				onContinuationFrame((ContinuationWebSocketFrame) frame);
			} else {
				logger.error("Invalid message {}", frame);
			}
		}
	}

	private void onPongFrame(PongWebSocketFrame pongWebSocketFrame) {
		byte[] bytes = getBytes(pongWebSocketFrame.content());

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onPongFrame(bytes);
			}
		});

	}

	private void onPingFrame(PingWebSocketFrame pingWebSocketFrame) {

		byte[] bytes = getBytes(pingWebSocketFrame.content());

		if (autoPong) {
			channel.writeAndFlush(new PongWebSocketFrame(Unpooled.copiedBuffer(bytes)));
		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onPingFrame(bytes);
			}

		});

	}

	private void onContinuationFrame(ContinuationWebSocketFrame continuationWebSocketFrame) {
		if (expectedFragmentedFrameType == null) {
			logger.error("Received continuation frame when the last frame was completed!");
			return;
		}

		try {
			switch (expectedFragmentedFrameType) {
				case BINARY:
					handleBinaryFrame(continuationWebSocketFrame);
					break;
				case TEXT:
					handleTextFrame(continuationWebSocketFrame);
					break;
				default:
					sendCloseFrame(1002, "Incorrect continuation frame!");
			}
		} finally {
			if (continuationWebSocketFrame.isFinalFragment()) {
				expectedFragmentedFrameType = null;
			}
		}
	}

	private void handleTextFrame(WebSocketFrame webSocketFrame) {
		try {
			String text = utf8Decoder.decode(webSocketFrame.content().nioBuffer()).toString();
			boolean finalFragment = webSocketFrame.isFinalFragment();
			int rsv = webSocketFrame.rsv();
			callbackExecutor.execute(() -> {
				for (WebSocketListener webSocketListener : listeners) {
					webSocketListener.onTextFrame(text, finalFragment, rsv);
				}

			});
		} catch (CharacterCodingException e) {
			sendCloseFrame(1007, "Invalid UTF-8 encoding");
		}

	}

	private void handleBinaryFrame(WebSocketFrame webSocketFrame) {
		byte[] bytes = getBytes(webSocketFrame.content());
		boolean finalFragment = webSocketFrame.isFinalFragment();
		int rsv = webSocketFrame.rsv();

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onBinaryFrame(bytes, finalFragment, rsv);
			}
		});
	}

	private void onBinaryFrame(BinaryWebSocketFrame binaryWebSocketFrame) {
		if (expectedFragmentedFrameType == null && !binaryWebSocketFrame.isFinalFragment()) {
			expectedFragmentedFrameType = FragmentedFrameType.BINARY;
		}
		handleBinaryFrame(binaryWebSocketFrame);
	}

	private void onTextFrame(TextWebSocketFrame textWebSocketFrame) {
		if (expectedFragmentedFrameType == null && !textWebSocketFrame.isFinalFragment()) {
			expectedFragmentedFrameType = FragmentedFrameType.TEXT;
		}

		handleTextFrame(textWebSocketFrame);
	}

	private void onClose(CloseWebSocketFrame closeWebSocketFrame) {
		int statusCode = closeWebSocketFrame.statusCode();
		String reasonText = closeWebSocketFrame.reasonText();

		if (autoCloseFrame) {
			sendCloseFrame(statusCode, reasonText);
		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onCloseFrame(statusCode, reasonText);
			}
		});


	}

	private void onOpen(Channel channel, io.netty.handler.codec.http.HttpHeaders httpHeaders) {
		this.channel = channel;
		this.headers = new Headers(httpHeaders);
		this.ready = true;
		CompletableFuture<WebSocketClient> future = this.connectionFuture;
		completableFutureExecutor.execute(() -> future.complete(this));
		this.connectionFuture = null;
		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onConnect(this);
			}
		});


		for (WebSocketFrame bufferedFrame : bufferedFrames) {
			onWebSocketFrame(bufferedFrame);
			bufferedFrame.release();
		}


		if (pingEveryDuration != null) {

			if (pingFuture != null) {
				pingFuture.cancel(true);
			}

			pingFuture = channel.eventLoop().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					sendPingFrame();
				}
			}, pingEveryDuration.toMillis(), pingEveryDuration.toMillis(), TimeUnit.MILLISECONDS);
		}

	}

	private void onError(HttpRequestContext httpRequestContext, Throwable throwable) {
		boolean wasConnected = ready;
		ready = false;
		channel = null;
		if (this.connectionFuture != null) {
			CompletableFuture<WebSocketClient> future = this.connectionFuture;

			completableFutureExecutor.execute(() -> future.completeExceptionally(throwable));
			this.connectionFuture = null;

		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onError(throwable);
			}
		});

		if (wasConnected) {
			callbackExecutor.execute(() -> {
				for (WebSocketListener webSocketListener : listeners) {
					webSocketListener.onDisconnect();
				}
			});
		}

		if (pingFuture != null) {
			pingFuture.cancel(true);
		}

		awaitLatch.closed();
	}

	private void onCompleted(HttpRequestContext httpRequestContext) {
		boolean wasConnected = ready;

		ready = false;
		channel = null;

		if (wasConnected) {
			callbackExecutor.execute(() -> {
				for (WebSocketListener webSocketListener : listeners) {
					webSocketListener.onDisconnect();
				}
			});
		}

		if (pingFuture != null) {
			pingFuture.cancel(true);
		}
		awaitLatch.closed();
	}

	public byte[] getBytes(ByteBuf buf) {
		int readable = buf.readableBytes();
		int readerIndex = buf.readerIndex();
		if (buf.hasArray()) {
			byte[] array = buf.array();
			if (buf.arrayOffset() == 0 && readerIndex == 0 && array.length == readable) {
				return array;
			}
		}
		byte[] array = new byte[readable];
		buf.getBytes(readerIndex, array);
		return array;
	}

	private enum FragmentedFrameType {
		TEXT, BINARY;
	}
}
