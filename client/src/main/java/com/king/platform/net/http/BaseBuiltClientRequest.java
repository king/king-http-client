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
	 * If the intention is to reuse this built request, make sure to use {@link #withHttpCallback(Supplier)} instead.
	 *
	 * @param httpCallback httpCallback the callback object
	 * @return this builder
	 */
	B withHttpCallback(HttpCallback<T> httpCallback);

	/**
	 * Each execution will use a HttpCallback supplied from the supplier.
	 * HttpCallbacks are executed on the HttpCallbackExecutor
	 *
	 * @param httpCallbackSupplier the supplier of httpCallbacks
	 * @return this builder
	 */
	B withHttpCallback(Supplier<HttpCallback<T>> httpCallbackSupplier);

	/**
	 * Specific nioCallback that will be executed for all requests.
	 * NioCallbacks are executed on the nio threads. Make sure to not do any blocking!.
	 * If the intention is to reuse this built request, make sure to use {@link #withNioCallback(Supplier)} instead.
	 * @param nioCallback the nio callback
	 * @return this builder
	 */
	B withNioCallback(NioCallback nioCallback);

	/**
	 * Each execution will use a NioCallback supplied from the supplier
	 * NioCallbacks are executed on the nio threads. Make sure to not do any blocking!.
	 * @param nioCallbackSupplier the supplier of nio callback
	 * @return this builder
	 */
	B withNioCallback(Supplier<NioCallback> nioCallbackSupplier);
}
