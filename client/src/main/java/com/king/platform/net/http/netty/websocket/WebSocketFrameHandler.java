package com.king.platform.net.http.netty.websocket;

import io.netty.handler.codec.TooLongFrameException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WebSocketFrameHandler {
	private final WebSocketListenerTrigger trigger;
	private final int maxAggregationSize;
	private final boolean legacyAggregateFrames;
	private final StringBuilder textBuffer = new StringBuilder();
	private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	private int currentSize = 0;

	public WebSocketFrameHandler(WebSocketListenerTrigger trigger, int maxAggregationSize, boolean legacyAggregateFrames) {
		this.trigger = trigger;
		this.maxAggregationSize = maxAggregationSize;
		this.legacyAggregateFrames = legacyAggregateFrames;
	}


	public void handleTextFrame(int byteSize, String text, boolean finalFragment, int rsv) {

		trigger.onTextFrame(text, finalFragment, rsv);
		if (!legacyAggregateFrames) {
			trigger.onLegacyTextFrame(text, finalFragment, rsv);
		}

		textBuffer.append(text);
		currentSize += byteSize;

		if (currentSize > maxAggregationSize) {
			reset();
			throw new TooLongFrameException();
		}

		if (finalFragment) {
			String completeText = textBuffer.toString();
			reset();

			trigger.onTextMessage(completeText);
			if (legacyAggregateFrames) {
				trigger.onLegacyTextFrame(completeText, true, 0);
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
		textBuffer.setLength(0);
		currentSize = 0;
	}

}
