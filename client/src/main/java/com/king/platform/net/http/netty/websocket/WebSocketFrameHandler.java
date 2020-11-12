package com.king.platform.net.http.netty.websocket;

import io.netty.handler.codec.TooLongFrameException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class WebSocketFrameHandler {
	private final CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);

	private final WebSocketListenerTrigger trigger;
	private final int maxAggregationSize;
	private final boolean legacyAggregateFrames;

	private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	private int currentSize = 0;

	public WebSocketFrameHandler(WebSocketListenerTrigger trigger, int maxAggregationSize, boolean legacyAggregateFrames) {
		this.trigger = trigger;
		this.maxAggregationSize = maxAggregationSize;
		this.legacyAggregateFrames = legacyAggregateFrames;
	}

	public void handleTextFrame(byte[] data, boolean finalFragment, int rsv) throws CharacterCodingException {
		trigger.onTextFrame(data, finalFragment, rsv);

		if (!legacyAggregateFrames) {
			String potentialPartialFrame = utf8Decoder.decode(ByteBuffer.wrap(data)).toString();
			trigger.onLegacyTextFrame(potentialPartialFrame, finalFragment, rsv);
		}

		currentSize += data.length;

		if (currentSize > maxAggregationSize) {
			reset();
			throw new TooLongFrameException();
		}

		try {
			byteArrayOutputStream.write(data);
		} catch (IOException ignored) {
		}
		if (finalFragment) {
			byte[] completeData = byteArrayOutputStream.toByteArray();
			String content = utf8Decoder.decode(ByteBuffer.wrap(completeData)).toString();
			reset();
			trigger.onTextMessage(content);
			if (legacyAggregateFrames) {
				trigger.onLegacyTextFrame(content, true, 0);
			}
		}

	}

	public void handleByteFrame(byte[] data, boolean finalFragment, int rsv) {
		trigger.onBinaryFrame(data, finalFragment, rsv);
		if (!legacyAggregateFrames) {
			trigger.onLegacyBinaryFrame(data, finalFragment, rsv);
		}

		currentSize += data.length;

		if (currentSize > maxAggregationSize) {
			reset();
			throw new TooLongFrameException();
		}

		try {
			byteArrayOutputStream.write(data);
			if (finalFragment) {
				byte[] completeData = byteArrayOutputStream.toByteArray();
				reset();
				trigger.onBinaryMessage(completeData);
				if (legacyAggregateFrames) {
					trigger.onLegacyBinaryFrame(completeData, true, 0);
				}
			}
		} catch (IOException ignored) {
		}
	}

	private void reset() {
		byteArrayOutputStream.reset();
		currentSize = 0;
	}

}
