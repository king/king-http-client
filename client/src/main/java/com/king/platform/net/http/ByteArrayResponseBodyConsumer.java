// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.nio.ByteBuffer;

public class ByteArrayResponseBodyConsumer implements ResponseBodyConsumer<byte[]> {
	private ByteAggregator byteAggregator;

	@Override
	public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
		byteAggregator = new ByteAggregator(contentLength);
	}

	@Override
	public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		byteAggregator.write(buffer);
	}

	@Override
	public void onCompletedBody() throws Exception {

	}

	@Override
	public byte[] getBody() {
		return byteAggregator.getBytes();
	}
}
