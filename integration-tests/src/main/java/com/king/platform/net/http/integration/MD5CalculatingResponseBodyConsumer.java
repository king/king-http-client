// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ResponseBodyConsumer;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;

public class MD5CalculatingResponseBodyConsumer implements ResponseBodyConsumer<byte[]> {

	private final MD5CalculatingOutputStream outputStream;
	private WritableByteChannel channel;

	public MD5CalculatingResponseBodyConsumer() {
		try {
			outputStream = new MD5CalculatingOutputStream();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
		channel = Channels.newChannel(outputStream);
	}

	@Override
	public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		try {
			channel.write(buffer);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onCompletedBody() throws Exception {
		channel.close();
	}

	@Override
	public byte[] getBody() {
		return outputStream.getMD5();
	}
}
