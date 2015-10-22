// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class ByteAggregator {

	private final ByteArrayOutputStream byteArrayOutputStream;
	private final WritableByteChannel channel;

	public ByteAggregator(long contentLength) {
		if (contentLength < 0) {
			contentLength = 1024;
		}

		byteArrayOutputStream = new ByteArrayOutputStream((int) contentLength);
		channel = Channels.newChannel(byteArrayOutputStream);

	}

	public void write(ByteBuffer buffer) throws IOException {
		channel.write(buffer);
	}

	public byte[] getBytes() {
		return byteArrayOutputStream.toByteArray();
	}
}
