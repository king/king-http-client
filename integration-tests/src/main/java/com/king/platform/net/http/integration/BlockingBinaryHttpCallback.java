// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.ResponseBodyConsumer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingBinaryHttpCallback implements HttpCallback<byte[]> {
	private HttpResponse<byte[]> httpResponse;
	private Throwable exception;

	private final CountDownLatch countDownLatch = new CountDownLatch(1);


	@Override
	public void onCompleted(HttpResponse<byte[]> httpResponse) {
		this.httpResponse = httpResponse;

		countDownLatch.countDown();
	}


	@Override
	public void onError(Throwable exception) {
		this.exception = exception;

		countDownLatch.countDown();
	}

	public int getStatusCode() {
		return httpResponse.getStatusCode();
	}

	public byte[] getBody() {
		if (httpResponse == null) {
			return null;
		}
		return httpResponse.getBody();
	}

	public String getHeader(String name) {
		return httpResponse.getHeader(name);
	}

	public Throwable getException() {
		return exception;
	}

	public void waitForCompletion() throws InterruptedException {
		countDownLatch.await();
	}

	public boolean waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		return countDownLatch.await(timeout, unit);
	}

	@Override
	public ResponseBodyConsumer<byte[]> newResponseBodyConsumer() {
		return new ResponseBodyConsumer<byte[]>() {
			private ByteArrayOutputStream outputStream;
			private WritableByteChannel channel;

			@Override
			public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
				outputStream = new ByteArrayOutputStream();
				channel = Channels.newChannel(outputStream);
			}

			@Override
			public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
				channel.write(buffer);
			}

			@Override
			public void onCompletedBody() throws Exception {
				channel.close();

			}

			@Override
			public byte[] getBody() {
				return outputStream.toByteArray();
			}
		};
	}
}
