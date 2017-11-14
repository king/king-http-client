package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.WebSocketClient;
import com.king.platform.net.http.WebSocketClientCallback;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.Event2;
import com.king.platform.net.http.netty.requestbuilder.BuiltNettyClientRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.slf4j.LoggerFactory.getLogger;

public class WebSocketClientImpl implements WebSocketClient {
	private final Logger logger = getLogger(getClass());

	private final WebSocketClientCallback webSocketClientCallback;
	private final BuiltNettyClientRequest<Void> builtNettyClientRequest;
	private final Executor callbackExecutor;
	private Channel channel;
	private HttpHeaders httpHeaders;
	private FragmentedFrameType expectedFragmentedFrameType;

	public WebSocketClientImpl(WebSocketClientCallback webSocketClientCallback, BuiltNettyClientRequest<Void> builtNettyClientRequest, Executor callbackExecutor) {
		this.webSocketClientCallback = webSocketClientCallback;
		this.builtNettyClientRequest = builtNettyClientRequest;
		this.callbackExecutor = callbackExecutor;

		builtNettyClientRequest.withCustomCallbackSupplier(requestEventBus -> {
			requestEventBus.subscribe(Event.onWsOpen, WebSocketClientImpl.this::onOpen);
			requestEventBus.subscribe(Event.onWsCloseFrame, WebSocketClientImpl.this::onClose);
			requestEventBus.subscribe(Event.onWsTextFrame, WebSocketClientImpl.this::onTextFrame);
			requestEventBus.subscribe(Event.onWsBinaryFrame, WebSocketClientImpl.this::onBinaryFrame);
			requestEventBus.subscribe(Event.onWsContinuationFrame, WebSocketClientImpl.this::onContinuationFrame);
			requestEventBus.subscribe(Event.onWsPingFrame, WebSocketClientImpl.this::onPingFrame);
			requestEventBus.subscribe(Event.onWsPongFrame, WebSocketClientImpl.this::onPongFrame);
		});

	}

	public void connect() {
		builtNettyClientRequest.execute();
	}

	@Override
	public CompletableFuture<Void> sendTextFrame(String text) {
		return convert(channel.writeAndFlush(new TextWebSocketFrame(text)));
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


	private void onPongFrame(Event1<PongWebSocketFrame> event, PongWebSocketFrame pongWebSocketFrame) {
		byte[] bytes = getBytes(pongWebSocketFrame.content());
		callbackExecutor.execute(() -> webSocketClientCallback.onPongFrame(bytes));
	}

	private void onPingFrame(Event1<PingWebSocketFrame> event, PingWebSocketFrame pingWebSocketFrame) {
		byte[] bytes = getBytes(pingWebSocketFrame.content());
		callbackExecutor.execute(() -> webSocketClientCallback.onPongFrame(bytes));
	}

	private void onContinuationFrame(Event1<ContinuationWebSocketFrame> event, ContinuationWebSocketFrame continuationWebSocketFrame) {
		if (expectedFragmentedFrameType == null) {
			logger.error("Received continuation frame when the last frame was completed!");
			return;
		}

		try {
			switch (expectedFragmentedFrameType) {
				case BINARY:
					onBinaryFrame(continuationWebSocketFrame);
					break;
				case TEXT:
					onTextFrame(continuationWebSocketFrame);
					break;
				default:
					throw new IllegalArgumentException("Unknown FragmentedFrameType " + expectedFragmentedFrameType);
			}
		} finally {
			if (continuationWebSocketFrame.isFinalFragment()) {
				expectedFragmentedFrameType = null;
			}
		}
	}

	private void onTextFrame(WebSocketFrame webSocketFrame) {
		String text = webSocketFrame.content().toString(CharsetUtil.UTF_8);

		boolean finalFragment = webSocketFrame.isFinalFragment();
		int rsv = webSocketFrame.rsv();

		callbackExecutor.execute(() -> webSocketClientCallback.onTextFrame(text, finalFragment, rsv));
	}

	private void onBinaryFrame(WebSocketFrame webSocketFrame) {
		byte[] bytes = getBytes(webSocketFrame.content());
		boolean finalFragment = webSocketFrame.isFinalFragment();
		int rsv = webSocketFrame.rsv();

		callbackExecutor.execute(() -> webSocketClientCallback.onBinaryFrame(bytes, finalFragment, rsv));
	}

	private void onBinaryFrame(Event1<BinaryWebSocketFrame> event, BinaryWebSocketFrame binaryWebSocketFrame) {
		if (expectedFragmentedFrameType == null && !binaryWebSocketFrame.isFinalFragment()) {
			expectedFragmentedFrameType = FragmentedFrameType.BINARY;
		}
		onBinaryFrame(binaryWebSocketFrame);
	}

	private void onTextFrame(Event1<TextWebSocketFrame> event, TextWebSocketFrame textWebSocketFrame) {
		if (expectedFragmentedFrameType == null && !textWebSocketFrame.isFinalFragment()) {
			expectedFragmentedFrameType = FragmentedFrameType.TEXT;
		}

		onTextFrame(textWebSocketFrame);
	}

	private void onClose(Event1<CloseWebSocketFrame> event, CloseWebSocketFrame closeWebSocketFrame) {
	}

	private void onOpen(Event2<Channel, HttpHeaders> event, Channel channel, HttpHeaders httpHeaders) {
		this.channel = channel;
		this.httpHeaders = httpHeaders;
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
