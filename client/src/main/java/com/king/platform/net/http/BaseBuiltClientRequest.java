package com.king.platform.net.http;


import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface BaseBuiltClientRequest<T, B extends BaseBuiltClientRequest<T, B>> {
	/**
	 * Execute the built request, consume the returned data as T.
	 * T is determined by the defined {@link ResponseBodyConsumer}
	 *
	 * @return a future with the result of the request
	 */
	CompletableFuture<HttpResponse<T>> execute();

	/**
	 * Specific httpCallback that will be executed for all requests
	 * HttpCallbacks are executed on the HttpCallbackExecutor
	 *
	 * @param httpCallback httpCallback the callback object
	 * @return this builder
	 */
	B withHttpCallback(HttpCallback<T> httpCallback);

	B withHttpCallback(Supplier<HttpCallback<T>> httpCallbackSupplier);

	B withNioCallback(NioCallback nioCallback);

	B withNioCallback(Supplier<NioCallback> nioCallbackSupplier);
}
