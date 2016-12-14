// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ResponseBodyStreamTest {
	@Test
	public void callbackShouldBeCalledForEachPercentage() throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		AtomicInteger percentageRef = new AtomicInteger();
		AtomicLong readLengthRef = new AtomicLong();
		AtomicLong contentLengthRef = new AtomicLong();

		WritableByteChannel channel = Channels.newChannel(new ByteArrayOutputStream());
		ResponseBodyStream responseBodyStream = new ResponseBodyStream(channel, new ResponseBodyStream.ProgressCallback() {
			@Override
			public void onProgress(int percentageCompleted, long readLength, long contentLength) {
				percentageRef.set(percentageCompleted);
				readLengthRef.set(readLength);
				contentLengthRef.set(contentLength);
			}
		});


		responseBodyStream.onBodyStart("content/type", "charset", 100);
		for (int i = 0; i < 100; i++) {
			responseBodyStream.onReceivedContentPart((ByteBuffer) ByteBuffer.allocate(1).put((byte) i).flip());
			assertEquals(100, contentLengthRef.longValue());
			assertEquals(i + 1, readLengthRef.longValue());
			assertEquals(i + 1, percentageRef.intValue());
		}
	}

	@Test
	public void bodyShouldWriteToChannelAndCloseIt() throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		WritableByteChannel channel = Channels.newChannel(buffer);

		ResponseBodyStream responseBodyStream = new ResponseBodyStream(channel, null);

		responseBodyStream.onBodyStart("content/type", "charset", 100);

		for (int i = 0; i < 100; i++) {
			responseBodyStream.onReceivedContentPart((ByteBuffer) ByteBuffer.allocate(1).put((byte) i).flip());
		}

		responseBodyStream.onCompletedBody();

		assertEquals(100, buffer.size());
		assertFalse(channel.isOpen());

	}
}
