// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.ResponseBodyConsumer;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingMD5CalculatingHttpCallback implements HttpCallback<byte[]> {
	private HttpResponse<byte[]> httpResponse;
	private Throwable exception;

	private CountDownLatch countDownLatch = new CountDownLatch(1);


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


	public Throwable getException() {
		return exception;
	}

	public void waitForCompletion() throws InterruptedException {
		countDownLatch.await();
	}

	public boolean waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		return countDownLatch.await(timeout, unit);
	}

	public ResponseBodyConsumer<byte[]> newResponseBodyConsumer() {
		final MD5CalculatingOutputStream outputStream;

		try {
			outputStream = new MD5CalculatingOutputStream();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}


		return new ResponseBodyConsumer<byte[]>() {


			private WritableByteChannel channel;

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
		};
	}
}
