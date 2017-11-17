package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.Headers;
import com.king.platform.net.http.WebSocketConnection;
import com.king.platform.net.http.WebSocketListener;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RunOnceCallback2;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.slf4j.LoggerFactory.getLogger;

public class WebSocketConnectionImpl implements WebSocketConnection {
	private final Logger logger = getLogger(getClass());

	private final CharsetDecoder utf8Decoder = Charset.forName("UTF8").newDecoder().onMalformedInput(CodingErrorAction.REPORT);

	private final WebSocketListener webSocketListener;
	private final BuiltNettyClientRequest<Void> builtNettyClientRequest;
	private final Executor callbackExecutor;
	private final Executor completableFutureExecutor;
	private final CompletableFuture<WebSocketConnection> completableFuture;
	private Channel channel;
	private Headers headers;
	private FragmentedFrameType expectedFragmentedFrameType;

	private boolean ready;
	private List<WebSocketFrame> bufferedFrames = new ArrayList<>();

	public WebSocketConnectionImpl(WebSocketListener webSocketListener, BuiltNettyClientRequest<Void> builtNettyClientRequest, Executor listenerExecutor, Executor completableFutureExecutor, CompletableFuture<WebSocketConnection> completableFuture) {
		this.webSocketListener = webSocketListener;
		this.builtNettyClientRequest = builtNettyClientRequest;
		this.callbackExecutor = listenerExecutor;
		this.completableFutureExecutor = completableFutureExecutor;
		this.completableFuture = completableFuture;

		builtNettyClientRequest.withCustomCallbackSupplier(requestEventBus -> {
			requestEventBus.subscribe(Event.onWsOpen, WebSocketConnectionImpl.this::onOpen);
			requestEventBus.subscribe(Event.onWsFrame, WebSocketConnectionImpl.this::onWebSocketFrame);
			requestEventBus.subscribe(Event.ERROR, new RunOnceCallback2<HttpRequestContext, Throwable>() {
				@Override
				public void onFirstEvent(HttpRequestContext payload1, Throwable payload2) {
					WebSocketConnectionImpl.this.onError(payload2);
				}
			});
		});

	}

	@Override
	public Headers headers() {
		return headers;
	}

	public void connect() {
		builtNettyClientRequest.execute();
	}

	@Override
	public CompletableFuture<Void> sendTextFrame(String text) {
		return convert(channel.writeAndFlush(new TextWebSocketFrame(text)));
	}


	@Override
	public CompletableFuture<Void> sendCloseFrame() {
		if (channel.isOpen()) {
			channel.writeAndFlush(new CloseWebSocketFrame());
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> sendBinaryFrame(byte[] payload) {
		return convert(channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload))));
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
		//no need to handle pong
	}

	private void onPingFrame(PingWebSocketFrame pingWebSocketFrame) {
		byte[] bytes = getBytes(pingWebSocketFrame.content());
		channel.writeAndFlush(new PongWebSocketFrame(Unpooled.copiedBuffer(bytes)));

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
					sendCloseFrame();
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
			callbackExecutor.execute(() -> webSocketListener.onTextFrame(text, finalFragment, rsv));
		} catch (CharacterCodingException e) {
			sendCloseFrame();
		}

	}

	private void handleBinaryFrame(WebSocketFrame webSocketFrame) {
		byte[] bytes = getBytes(webSocketFrame.content());
		boolean finalFragment = webSocketFrame.isFinalFragment();
		int rsv = webSocketFrame.rsv();

		callbackExecutor.execute(() -> webSocketListener.onBinaryFrame(bytes, finalFragment, rsv));
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
		callbackExecutor.execute(() -> webSocketListener.onDisconnect(statusCode, reasonText));
	}

	private void onOpen(Channel channel, io.netty.handler.codec.http.HttpHeaders httpHeaders) {
		this.channel = channel;
		this.headers = new Headers(httpHeaders);

		completableFutureExecutor.execute(() -> completableFuture.complete(this));

		callbackExecutor.execute(() -> webSocketListener.onConnect(this));
		this.ready = true;

		for (WebSocketFrame bufferedFrame : bufferedFrames) {
			onWebSocketFrame(bufferedFrame);
			bufferedFrame.release();
		}
	}

	private void onError(Throwable throwable) {
		completableFutureExecutor.execute(() -> completableFuture.completeExceptionally(throwable));

		callbackExecutor.execute(() -> webSocketListener.onError(throwable));
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
