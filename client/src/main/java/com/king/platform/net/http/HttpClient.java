// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

import java.util.concurrent.Executor;

/**
 * Interface used to create http request builders.
 * <p>
 * Instance of this class can be created with {@link com.king.platform.net.http.netty.NettyHttpClientBuilder} <br>
 */
public interface HttpClient {

	/**
	 * Start the http client.
	 * Make sure that all {@link ConfKeys} is set before.
	 *
	 * @throws IllegalStateException If the client is already started when this method is invoked.
	 */
	void start();

	/**
	 * Stop the http client. The client can't be restarted after it has been shutdown.
	 *
	 * @throws IllegalStateException If the client isn't running when this method is invoked.
	 */
	void shutdown();


	/**
	 * Is the client started or not?
	 *
	 * @return true if client is started
	 */
	boolean isStarted();

	/**
	 * Create reusable builder for http get requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestBuilder createGet(String uri);

	/**
	 * Create reusable builder for http post requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestWithBodyBuilder}
	 */
	HttpClientRequestWithBodyBuilder createPost(String uri);

	/**
	 * Create reusable builder for http put requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestWithBodyBuilder}
	 */
	HttpClientRequestWithBodyBuilder createPut(String uri);

	/**
	 * Create reusable builder for http delete requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestBuilder createDelete(String uri);

	/**
	 * Create reusable builder for http head requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestBuilder createHead(String uri);

	/**
	 * Create reusable builder for http options requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestBuilder createOptions(String uri);

	/**
	 * Create reusable builder for http trace requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestBuilder createTrace(String uri);

	/**
	 * Create reusable builder for http patch requests. The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientRequestBuilder}
	 */
	HttpClientRequestWithBodyBuilder createPatch(String uri);

	/**
	 * Create reusable builder for http server side events.The client has to be started before this method is called.
	 *
	 * @param uri Http uri to call
	 * @return The reusable {@link HttpClientSseRequestBuilder}
	 */
	HttpClientSseRequestBuilder createSSE(String uri);

	/**
	 * Create reusable builder for web socket connections. The client has to be started before this method is called.
	 * The default behavior is that {@link WebSocketListener} is called on the nio threads.
	 * This behavior can be overridden by supplying an executor on {@link HttpClientWebSocketRequestBuilder#executingOn(Executor)}
	 *
	 * @param uri the ws/wss uri to call
	 * @return the builder
	 */
	HttpClientWebSocketRequestBuilder createWebSocket(String uri);
}
