package com.king.platform.net.http;


import java.util.concurrent.CompletableFuture;

public interface BaseBuiltClientRequest<T, B extends BaseBuiltClientRequest<T, B>> {
	/**
	 * Execute the built request, consume the returned data as String
	 *
	 * @return a future with the result of the request
	 */
	CompletableFuture<HttpResponse<T>> execute();

	B withHttpCallback(HttpCallback<T> httpCallback);

	B withNioCallback(NioCallback nioCallback);
}
