// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.FutureResult;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.*;

import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResponseFuture<T> implements Future<FutureResult<T>> {
	private final AtomicBoolean done = new AtomicBoolean();
	private final AtomicBoolean canceled = new AtomicBoolean();
	private final CountDownLatch latch = new CountDownLatch(1);

	private final RequestEventBus requestEventBus;
	private final HttpRequestContext requestContext;

	private FutureResult<T> result;

	public ResponseFuture(RequestEventBus requestEventBus, HttpRequestContext requestContext) {
		this.requestEventBus = requestEventBus;
		this.requestContext = requestContext;

		requestEventBus.subscribe(Event.ERROR, new RunOnceCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onFirstEvent(Event2 event, HttpRequestContext requestContext, Throwable throwable) {
				if ((throwable instanceof CancellationException || !canceled.get()) && done.compareAndSet(false, true)) {
					result = new FutureResult<>(throwable);
					latch.countDown();
				}
			}
		});

		requestEventBus.subscribe(Event.onHttpResponseDone, new RunOnceCallback1<HttpResponse>() {
			@Override
			public void onFirstEvent(Event1 event, HttpResponse payload) {
				if (!canceled.get() && done.compareAndSet(false, true)) {
					result = new FutureResult<>(payload);
					latch.countDown();
				}
			}
		});
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (!done.get() && canceled.compareAndSet(false, true)) {
			requestEventBus.triggerEvent(Event.ERROR, requestContext, new CancellationException());
		}
		return true;
	}

	@Override
	public boolean isCancelled() {
		return canceled.get();
	}

	@Override
	public boolean isDone() {
		return done.get();
	}

	@Override
	public FutureResult<T> get() throws InterruptedException, ExecutionException {
		latch.await();

		return result;
	}

	@Override
	public FutureResult<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!latch.await(timeout, unit)) {
			throw new TimeoutException();
		}

		return result;
	}

	protected static <T> ResponseFuture<T> error(Throwable error) {
		ResponseFuture<T> future = new ResponseFuture<>(new NoopRequestEventBus(), null);
		future.result = new FutureResult<>(error);
		future.done.set(true);
		future.latch.countDown();
		return future;
	}
}
