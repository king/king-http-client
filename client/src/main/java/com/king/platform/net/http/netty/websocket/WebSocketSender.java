package com.king.platform.net.http.netty.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WebSocketSender {
	private final int maxOutgoingFrameSize;

	private NextContiuationFrame nextContiuationFrame;

	public WebSocketSender(int maxOutgoingFrameSize) {
		this.maxOutgoingFrameSize = maxOutgoingFrameSize;
	}

	public CompletableFuture<Void> sendTextMessage(Channel channel, String text) {
		if (nextContiuationFrame != null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last frame was an continuation frame. A final frame has to be sent before an new complete is sent!"));
			return future;
		}

		if (text == null) {
			text = "";
		}

		if (text.length() * 4 <= maxOutgoingFrameSize) {  //max utf-8 size of this text is less then maxOutgoingFrameSize, we can send it directly
			return sendTextFrame(channel, text, true, 0);

		}

		//otherwise we might have to split it up!

		List<WebSocketFrame> webSocketFrames = new ArrayList<>();

		Charset charset = StandardCharsets.UTF_8;
		CharsetEncoder charsetEncoder = charset.newEncoder();
		CharBuffer charBuffer = CharBuffer.wrap(text);

		boolean first = true;
		boolean lastFrame = false;

		do {
			byte[] content = new byte[maxOutgoingFrameSize];
			ByteBuffer buffer = ByteBuffer.wrap(content);
			CoderResult cr = charsetEncoder.encode(charBuffer, buffer, true);

			int length = buffer.position();

			if (!cr.isOverflow()) {
				lastFrame = true;
			}

			if (first) {
				nextContiuationFrame = NextContiuationFrame.TEXT;
				WebSocketFrame webSocketFrame = nextContiuationFrame.create(lastFrame, 0, Unpooled.wrappedBuffer(content, 0, length));
				webSocketFrames.add(webSocketFrame);
				nextContiuationFrame = NextContiuationFrame.CONTINUATION_TEXT;
				first = false;
			} else {
				WebSocketFrame webSocketFrame = nextContiuationFrame.create(lastFrame, 0, Unpooled.wrappedBuffer(content, 0, length));
				webSocketFrames.add(webSocketFrame);
			}

		} while (!lastFrame);

		nextContiuationFrame = null;

		CompletableFuture<Void>[] cf = new CompletableFuture[webSocketFrames.size()];
		for (int i = 0; i < webSocketFrames.size(); i++) {
			cf[i] = convert(channel.write(webSocketFrames.get(i)));
		}

		channel.flush();

		return CompletableFuture.allOf(cf);


	}

	public CompletableFuture<Void> sendTextFrame(Channel channel, String text, boolean finalFragment, int rsv) {
		ByteBuf binaryData = Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
		return sendTextFrame(channel, binaryData, finalFragment, rsv);
	}

	public CompletableFuture<Void> sendTextFrame(Channel channel, ByteBuf binaryData, boolean finalFragment, int rsv) {
		if (nextContiuationFrame != null && !nextContiuationFrame.allowedFrame(FrameType.TEXT)) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last sent continuation frame was of an different type!"));
			return future;
		}

		if (binaryData.readableBytes() > maxOutgoingFrameSize) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Frame payload is larger then maxOutgoingFrameSize"));
			binaryData.release();
			return future;
		}

		if (nextContiuationFrame == null) {
			nextContiuationFrame = NextContiuationFrame.TEXT;
		}

		WebSocketFrame webSocketFrame = nextContiuationFrame.create(finalFragment, rsv, binaryData);
		if (finalFragment) {
			nextContiuationFrame = null;
		} else {
			nextContiuationFrame = NextContiuationFrame.CONTINUATION_TEXT;
		}

		return convert(channel.writeAndFlush(webSocketFrame));

	}

	public CompletableFuture<Void> sendBinaryMessage(Channel channel, byte[] payload) {
		if (nextContiuationFrame != null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last frame was an continuation frame. A final frame has to be sent before an new complete is sent!"));
			return future;
		}

		if (payload == null || payload.length <= maxOutgoingFrameSize) {
			return sendBinaryFrame(channel, payload, true, 0);
		}

		ByteBuf buffer = Unpooled.copiedBuffer(payload);

		List<WebSocketFrame> webSocketFrames = new ArrayList<>();

		int readIndex = buffer.readerIndex();
		int sizeLeft = buffer.readableBytes();
		boolean first = true;
		while (sizeLeft != 0) {
			int size = Math.min(sizeLeft, maxOutgoingFrameSize);
			sizeLeft -= size;
			boolean lastFrame = sizeLeft == 0;
			if (first) {
				nextContiuationFrame = NextContiuationFrame.BINARY;
				webSocketFrames.add(nextContiuationFrame.create(lastFrame, 0, buffer.slice(readIndex, size)));
				first = false;
				nextContiuationFrame = NextContiuationFrame.CONTINUATION_BINARY;
			} else {
				webSocketFrames.add(nextContiuationFrame.create(lastFrame, 0, buffer.slice(readIndex, size)));
			}
			buffer.retain();
			readIndex += size;
		}

		CompletableFuture<Void>[] cf = new CompletableFuture[webSocketFrames.size()];
		for (int i = 0; i < webSocketFrames.size(); i++) {
			cf[i] = convert(channel.write(webSocketFrames.get(i)));
		}

		channel.flush();

		nextContiuationFrame = null;

		return CompletableFuture.allOf(cf);
	}

	public CompletableFuture<Void> sendBinaryFrame(Channel channel, byte[] payload, boolean finalFragment, int rsv) {
		if (payload.length > maxOutgoingFrameSize) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Frame payload is larger then maxOutgoingFrameSize"));
			return future;
		}


		if (nextContiuationFrame != null && !nextContiuationFrame.allowedFrame(FrameType.BINARY)) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last sent continuation frame was of an different type!"));
			return future;
		}

		if (nextContiuationFrame == null) {
			nextContiuationFrame = NextContiuationFrame.BINARY;
		}

		WebSocketFrame webSocketFrame = nextContiuationFrame.create(finalFragment, rsv, Unpooled.copiedBuffer(payload));
		if (finalFragment) {
			nextContiuationFrame = null;
		} else {
			nextContiuationFrame = NextContiuationFrame.CONTINUATION_BINARY;
		}

		return convert(channel.writeAndFlush(webSocketFrame));
	}

	public CompletableFuture<Void> sendBinaryFrame(Channel channel, byte[] payload, boolean finalFragment, int offset, int length, int rsv) {
		if (payload.length > maxOutgoingFrameSize) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Frame payload is larger then maxOutgoingFrameSize"));
			return future;
		}


		if (nextContiuationFrame != null && !nextContiuationFrame.allowedFrame(FrameType.BINARY)) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("Last sent continuation frame was of an different type!"));
			return future;
		}

		if (nextContiuationFrame == null) {
			nextContiuationFrame = NextContiuationFrame.BINARY;
		}

		WebSocketFrame webSocketFrame = nextContiuationFrame.create(finalFragment, rsv, Unpooled.copiedBuffer(payload, offset, length));
		if (finalFragment) {
			nextContiuationFrame = null;
		} else {
			nextContiuationFrame = NextContiuationFrame.CONTINUATION_BINARY;
		}

		return convert(channel.writeAndFlush(webSocketFrame));
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

	enum FrameType {
		TEXT,
		BINARY,
	}

	enum NextContiuationFrame {
		TEXT(FrameType.TEXT) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new TextWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},
		BINARY(FrameType.BINARY) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new BinaryWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},

		CONTINUATION_TEXT(FrameType.TEXT) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new ContinuationWebSocketFrame(finalFragment, rsv, binaryData);
			}
		},
		CONTINUATION_BINARY(FrameType.BINARY) {
			@Override
			public WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData) {
				return new ContinuationWebSocketFrame(finalFragment, rsv, binaryData);
			}
		};

		private final FrameType allowedPreviousFrameTypes;

		NextContiuationFrame(FrameType allowedPreviousFrameTypes) {
			this.allowedPreviousFrameTypes = allowedPreviousFrameTypes;
		}


		public boolean allowedFrame(FrameType frameType) {
			return allowedPreviousFrameTypes == frameType;
		}

		public abstract WebSocketFrame create(boolean finalFragment, int rsv, ByteBuf binaryData);

	}

}
