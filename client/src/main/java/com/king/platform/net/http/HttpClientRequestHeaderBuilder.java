// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.Map;
import java.util.concurrent.Executor;

public interface HttpClientRequestHeaderBuilder<T extends HttpClientRequestHeaderBuilder> {

	/**
	 * Add custom header to the request
	 * @param name Header name
	 * @param value Header value
	 * @return the builder
	 */
	T addHeader(CharSequence name, CharSequence value);


	/**
	 * Add a map of custom headers to the request
	 * @param headers Key Value map of header values
	 * @return the builder
	 */
	T addHeaders(Map<CharSequence, CharSequence> headers);

	/**
	 * Set if the connection should be kept alive or not.<br>
	 * If keepAlive is false, header "Connection" is set to false. <br>
	 * Overrides {@link ConfKeys#KEEP_ALIVE}
	 * @param keepAlive true if the connection should be kept alive against the server
	 * @return the builder
	 */
	T keepAlive(boolean keepAlive);

	/**
	 * Set the timeout for how long the idle socket should be kept around if keepAlive is used.
	 * By default, this is controlled by {@link com.king.platform.net.http.netty.NettyHttpClientBuilder#setKeepAliveTimeoutMs(int)}
	 * This is only set on newly opened connections. If a connection is reused, the idle timeout on it will not be changed.
	 * @param keepAliveTimeoutMillis  the time after which the connection will be closed (in ms)
	 * @return the builder
	 */
	T keepAliveTimeout(int keepAliveTimeoutMillis);

	/**
	 * Set if the client should accept compressed responses from the server <br>
	 * Overrides {@link ConfKeys#ACCEPT_COMPRESSED_RESPONSE}
	 * @param acceptCompressedResponse true if the client accepts compressed responses
	 * @return the builder
	 */
	T acceptCompressedResponse(boolean acceptCompressedResponse);



	/**
	 * Set the idle timeout in milliseconds <br>
	 * Overrides {@link ConfKeys#IDLE_TIMEOUT_MILLIS}
	 * @param idleTimeoutMillis the idle timeout in milliseconds
	 * @return the builder
	 */
	T idleTimeoutMillis(int idleTimeoutMillis);

	/**
	 * Set the total request timeout in milliseconds <br>
	 * Overrides {@link ConfKeys#TOTAL_REQUEST_TIMEOUT_MILLIS}
	 * @param totalRequestTimeoutMillis the total request timeout in milliseconds
	 * @return the builder
	 */
	T totalRequestTimeoutMillis(int totalRequestTimeoutMillis);

	/**
	 * Set if the request should automatically follow redirects (up to 5 redirects). <br>
	 * Overrides {@link ConfKeys#HTTP_FOLLOW_REDIRECTS}
	 * @param followRedirects if the request should follow redirects
	 * @return the builder
	 */
	T followRedirects(boolean followRedirects);


	/**
	 * Add one or more query parameter to the current uri.
	 * @param name name of the query parameter
	 * @param value value of the query parameter
	 * @return the builder
	 */
	T addQueryParameter(String name, String value);


	/**
	 * Add a map of query parameters to the current uri
	 * @param parameters the map of query parameters
	 * @return the builder
	 */
	T addQueryParameters(Map<String, String> parameters);

	/**
	 * Provide a custom executor that will be used for this request.
	 * For example an single threaded executor can be used to guarantee the order of the events.
	 * @param executor the executor
	 * @return the builder
	 */
	T executingOn(Executor executor);

	/**
	 * Set if the response should automatically be decompressed (if gziped).
	 * Overrides {@link ConfKeys#AUTOMATICALLY_DECOMPRESS_RESPONSE}
	 * @param automaticallyDecompressResponse if the response should be decompressed
	 * @return the builder
	 */
	T automaticallyDecompressResponse(boolean automaticallyDecompressResponse);
}
