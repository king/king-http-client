package com.king.platform.net.http.netty;


import com.king.platform.net.http.HttpCallback;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.Event2;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpCallbackInvoker<T> {
	private final AtomicBoolean firstExecute = new AtomicBoolean();
	private final Executor callbackExecutor;
	private final HttpCallback<T> httpCallback;

	public HttpCallbackInvoker(Executor callbackExecutor, HttpCallback<T> httpCallback) {
		this.callbackExecutor = callbackExecutor;
		this.httpCallback = httpCallback;
	}


	public void onHttpResponseDone(Event1<HttpResponse> event, HttpResponse httpResponse) {
		if (firstExecute.compareAndSet(false, true)) {
			callbackExecutor.execute(() -> httpCallback.onCompleted(httpResponse));
		}
	}

	public void onError(Event2<HttpRequestContext, Throwable> event, HttpRequestContext httpRequestContext, Throwable throwable) {
		if (firstExecute.compareAndSet(false, true)) {
			callbackExecutor.execute(() -> httpCallback.onError(throwable));
		}
	}
}
