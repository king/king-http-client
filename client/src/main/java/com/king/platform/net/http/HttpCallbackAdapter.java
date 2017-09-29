package com.king.platform.net.http;


public interface HttpCallbackAdapter<T> extends HttpCallback<T> {
	@Override
	default void onCompleted(HttpResponse<T> httpResponse) {

	}

	@Override
	default void onError(Throwable throwable) {

	}
}
