// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingBinaryHttpCallback implements HttpCallback<byte[]> {
	private volatile HttpResponse<byte[]> httpResponse;
	private volatile Throwable exception;

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


}
