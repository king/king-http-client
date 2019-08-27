package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.Headers;
import com.king.platform.net.http.WebSocketClient;
import com.king.platform.net.http.WebSocketListener;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.NettyHeaders;
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
import io.netty.util.CharsetUtil;
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
import java.util.concurrent.locks.ReentrantLock;

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

	private final boolean splitLargeFrames;
	private final int maxFrameSize;

	private final Duration pingEveryDuration;
	private final AwaitLatch awaitLatch = new AwaitLatch();
	private final ReentrantLock lock;
	private Headers headers = new NettyHeaders(EmptyHttpHeaders.INSTANCE);
	private FrameType expectedIncomingFragmentedFrameType;
	private NextContiuationFrame nextContiuationFrame;

	private List<WebSocketFrame> bufferedFrames = new ArrayList<>();
	private volatile Channel channel;
	private volatile boolean ready;
	private volatile CompletableFuture<WebSocketClient> connectionFuture;
	private volatile ScheduledFuture<?> pingFuture;

	public WebSocketClientImpl(BuiltNettyClientRequest<Void> builtNettyClientRequest, Executor listenerExecutor, Executor completableFutureExecutor,
							   boolean autoCloseFrame, boolean splitLargeFrames, int maxFrameSize, boolean
								   autoPong, Duration pingEveryDuration) {
		this.builtNettyClientRequest = builtNettyClientRequest;
		this.callbackExecutor = listenerExecutor;
		this.completableFutureExecutor = completableFutureExecutor;
		this.splitLargeFrames = splitLargeFrames;
		this.maxFrameSize = maxFrameSize;
		this.autoPong = autoPong;
		this.autoCloseFrame = autoCloseFrame;
		this.pingEveryDuration = pingEveryDuration;

		lock = new ReentrantLock();

		builtNettyClientRequest.withCustomCallbackSupplier(requestEventBus -> {
			requestEventBus.subscribe(Event.onAttachedToChannel, currentChannel -> channel = currentChannel);
			requestEventBus.subscribe(Event.onWsOpen, WebSocketClientImpl.this::onOpen);
			requestEventBus.subscribe(Event.onWsFrame, WebSocketClientImpl.this::onWebSocketFrame);
			requestEventBus.subscribe(Event.ERROR, WebSocketClientImpl.this::onError);
			requestEventBus.subscribe(Event.COMPLETED, WebSocketClientImpl.this::onCompleted);
			requestEventBus.subscribe(Event.POPULATE_CONNECTION_SPECIFIC_HEADERS, WebSocketUtil::populateHeaders);
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
	public boolean isConnected() {
		return ready;
	}

	@Override
	public CompletableFuture<Void> sendTextFrame(String text) {
		ByteBuf buf = Unpooled.EMPTY_BUFFER;
		if (text != null && !text.isEmpty()) {
			buf = Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
		}
		return sendFrame(buf, FrameType.TEXT);
	}

	private CompletableFuture<Void> sendFrame(ByteBuf buffer, FrameType frameType) {
		Channel channel = this.channel;

		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		int length = buffer.readableBytes();

		if (length > maxFrameSize && !splitLargeFrames) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Payload size is larger then maxFrameSize and splitLargeFrames is disabled"));
			return future;
		}

		if (nextContiuationFrame != null && nextContiuationFrame.fragmentedFrame) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last frame was an continuation frame. A final frame has to be sent before an new complete is sent!"));
			return future;
		}


		if (length > maxFrameSize) {
			List<WebSocketFrame> webSocketFrames = new ArrayList<>();

			int readIndex = buffer.readerIndex();
			int sizeLeft = buffer.readableBytes();
			boolean first = true;
			while (sizeLeft != 0) {
				int size = Math.min(sizeLeft, maxFrameSize);
				sizeLeft -= size;
				boolean lastFrame = sizeLeft == 0;
				if (first) {
					if (frameType == FrameType.TEXT) {
						webSocketFrames.add(new TextWebSocketFrame(lastFrame, 0, buffer.slice(readIndex, size)));
					} else if (frameType == FrameType.BINARY) {
						webSocketFrames.add(new BinaryWebSocketFrame(lastFrame, 0, buffer.slice(readIndex, size)));
					}
					first = false;
				} else {
					webSocketFrames.add(new ContinuationWebSocketFrame(lastFrame, 0, buffer.slice(readIndex, size)));
				}
				buffer.retain();
				readIndex += size;
			}

			CompletableFuture<Void>[] cf = new CompletableFuture[webSocketFrames.size()];
			for (int i = 0; i < webSocketFrames.size(); i++) {
				cf[i] = convert(this.channel.write(webSocketFrames.get(i)));
			}

			this.channel.flush();

			nextContiuationFrame = null;

			return CompletableFuture.allOf(cf);

		} else {
			if (frameType == FrameType.TEXT) {
				return convert(channel.writeAndFlush(new TextWebSocketFrame(buffer)));
			} else {
				return convert(channel.writeAndFlush(new BinaryWebSocketFrame(buffer)));
			}
		}
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

	@Override
	public CompletableFuture<Void> sendTextFrame(String text, boolean finalFragment, int rsv) {

		ByteBuf buf = Unpooled.EMPTY_BUFFER;
		if (text != null && !text.isEmpty()) {
			buf = Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
		}
		return sendFrame(buf, finalFragment, rsv, FrameType.TEXT);

	}

	private CompletableFuture<Void> sendFrame(ByteBuf buf, boolean finalFragment, int rsv, FrameType frameType) {
		Channel channel = this.channel;

		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		if (nextContiuationFrame != null && !nextContiuationFrame.allowedFrame(frameType)) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last sent continuation frame was of an different type!"));
			return future;
		}

		int length = buf.readableBytes();

		if (length > maxFrameSize) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Payload size is larger then maxFrameSize"));
			return future;
		}

		NextContiuationFrame nextFrame = nextContiuationFrame;
		if (frameType == FrameType.TEXT && nextContiuationFrame == null) {
			nextContiuationFrame = NextContiuationFrame.TEXT;
			nextFrame =  NextContiuationFrame.CONTINUATION_TEXT;
		} else if (frameType == FrameType.BINARY && nextContiuationFrame == null) {
			nextContiuationFrame = NextContiuationFrame.BINARY;
			nextFrame =  NextContiuationFrame.CONTINUATION_BINARY;
		}

		WebSocketFrame webSocketFrame = nextContiuationFrame.create(finalFragment, rsv, buf);

		if (finalFragment){
			nextContiuationFrame = null;
		} else {
			nextContiuationFrame = nextFrame;
		}

		return convert(channel.writeAndFlush(webSocketFrame));

	}

	@Override
	public CompletableFuture<Void> sendCloseFrame(int statusCode, String reason) {
		Channel channel = this.channel;

		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		channel.writeAndFlush(new CloseWebSocketFrame(statusCode, reason));

		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> sendCloseFrame() {
		return sendCloseFrame(1000, "");
	}

	@Override
	public CompletableFuture<Void> sendBinaryFrame(byte[] payload) {
		if (payload == null) {
			payload = new byte[0];
		}

		return sendFrame(Unpooled.copiedBuffer(payload), FrameType.BINARY);
	}

	@Override
	public CompletableFuture<Void> sendBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
		if (payload == null) {
			payload = new byte[0];
		}

		return sendFrame(Unpooled.copiedBuffer(payload), finalFragment, rsv, FrameType.BINARY);
	}

	@Override
	public CompletableFuture<Void> sendPingFrame() {
		Channel channel = this.channel;
		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PingWebSocketFrame()));
	}

	@Override
	public CompletableFuture<Void> sendPingFrame(byte[] payload) {
		Channel channel = this.channel;
		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer(payload))));
	}

	@Override
	public CompletableFuture<Void> sendPongFrame() {
		Channel channel = this.channel;
		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PongWebSocketFrame()));
	}

	@Override
	public CompletableFuture<Void> sendPongFrame(byte[] payload) {
		Channel channel = this.channel;
		if (!ready || channel == null || !channel.isOpen()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Not connected!"));
			return future;
		}

		return convert(channel.writeAndFlush(new PongWebSocketFrame(Unpooled.copiedBuffer(payload))));
	}

	@Override
	public CompletableFuture<Void> close() {
		lock.lock();
		try {
			if (connectionFuture != null) {
				connectionFuture.complete(this);
			}

			ready = false;

			if (channel != null) {
				Channel activeChannel = channel;
				channel = null;
				return convert(activeChannel.close());
			}

			return CompletableFuture.completedFuture(null);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void addListener(WebSocketListener webSocketListener) {
		listeners.add(webSocketListener);
	}

	@Override
	public void awaitClose() throws InterruptedException {
		if (connectionFuture == null && channel == null) {
			return;
		}

		awaitLatch.awaitClose();
	}

	@Override
	public CompletableFuture<WebSocketClient> connect() {
		lock.lock();
		try {
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
		} finally {
			lock.unlock();
		}

	}

	private void onWebSocketFrame(WebSocketFrame frame) {
		if (!ready) {
			bufferedFrames.add(frame);
			frame.retain();
		} else {
			try {
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
			} finally {
				frame.release();
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

		Channel channel = this.channel;
		if (autoPong && channel != null && channel.isOpen()) {
			channel.writeAndFlush(new PongWebSocketFrame(Unpooled.copiedBuffer(bytes)));
		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onPingFrame(bytes);
			}

		});

	}

	private void onContinuationFrame(ContinuationWebSocketFrame continuationWebSocketFrame) {
		if (expectedIncomingFragmentedFrameType == null) {
			logger.error("Received continuation frame when the last frame was completed!");
			return;
		}

		try {
			switch (expectedIncomingFragmentedFrameType) {
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
				expectedIncomingFragmentedFrameType = null;
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
		if (expectedIncomingFragmentedFrameType == null && !binaryWebSocketFrame.isFinalFragment()) {
			expectedIncomingFragmentedFrameType = FrameType.BINARY;
		}
		handleBinaryFrame(binaryWebSocketFrame);
	}

	private void onTextFrame(TextWebSocketFrame textWebSocketFrame) {
		if (expectedIncomingFragmentedFrameType == null && !textWebSocketFrame.isFinalFragment()) {
			expectedIncomingFragmentedFrameType = FrameType.TEXT;
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
		CompletableFuture<WebSocketClient> future = this.connectionFuture;

		lock.lock();

		try {
			this.channel = channel;
			this.headers = new NettyHeaders(httpHeaders);
			this.ready = true;
			this.connectionFuture = null;

		} finally {
			lock.unlock();
		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onConnect(this);
			}
		});


		if (future != null) {
			completableFutureExecutor.execute(() -> future.complete(this));
		}


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
		CompletableFuture<WebSocketClient> future = this.connectionFuture;
		boolean wasConnected = ready;

		lock.lock();
		try {
			channel = null;
			ready = false;
			this.connectionFuture = null;
		} finally {
			lock.unlock();
		}

		callbackExecutor.execute(() -> {
			for (WebSocketListener webSocketListener : listeners) {
				webSocketListener.onError(throwable);
			}
		});

		if (future != null) {
			completableFutureExecutor.execute(() -> future.completeExceptionally(throwable));
		}

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
		boolean wasConnected;

		lock.lock();

		try {
			wasConnected = ready;
			ready = false;
			channel = null;
		} finally {
			lock.unlock();
		}


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

	@Override
	public String toString() {
		return "WebSocketClientImpl{" +
			"channel=" + channel +
			", ready=" + ready +
			'}';
	}

	enum FrameType {
		TEXT,
		BINARY,
	}

	enum NextContiuationFrame {
		TEXT(false, FrameType.TEXT, FrameType.BINARY) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new TextWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},
		BINARY(false,FrameType.TEXT, FrameType.BINARY) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new BinaryWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},

		CONTINUATION_TEXT(true, FrameType.TEXT) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new ContinuationWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},
		CONTINUATION_BINARY(true, FrameType.BINARY) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new ContinuationWebSocketFrame(finalFragment, rsv, binaryData);
			}
		};

		private final boolean fragmentedFrame;
		private final FrameType[] allowedPreviousFrameTypes;

		NextContiuationFrame(boolean fragmentedFrame, FrameType... allowedPreviousFrameTypes) {
			this.fragmentedFrame = fragmentedFrame;
			this.allowedPreviousFrameTypes = allowedPreviousFrameTypes;
		}


		public boolean allowedFrame(FrameType frameType) {
			for (FrameType allowedPreviousFrameType : allowedPreviousFrameTypes) {
				if (allowedPreviousFrameType == frameType) {
					return true;
				}
			}
			return false;
		}

		public abstract WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData);

	}

}
