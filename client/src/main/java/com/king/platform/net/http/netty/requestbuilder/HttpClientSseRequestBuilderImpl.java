package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltSseClientRequest;
import com.king.platform.net.http.HttpClientSseRequestBuilder;
import com.king.platform.net.http.SseExecutionCallback;
import com.king.platform.net.http.SseClient;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import com.king.platform.net.http.netty.sse.SseClientImpl;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.Executor;

public class HttpClientSseRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientSseRequestBuilder> implements HttpClientSseRequestBuilder {
	public HttpClientSseRequestBuilderImpl(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {
		super(HttpClientSseRequestBuilder.class, nettyHttpClient, httpVersion, httpMethod, uri, confMap);
	}

	@Override
	public BuiltSseClientRequest build() {

		withHeader("Accept", "text/event-stream");

		final BuiltNettyClientRequest builtNettyClientRequest = new BuiltNettyClientRequest(nettyHttpClient, httpVersion, httpMethod, uri, defaultUserAgent,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, acceptCompressedResponse, keepAlive, null, null, null, queryParameters,
			headerParameters);

		Executor httpClientCallbackExecutor = nettyHttpClient.getHttpClientCallbackExecutor();

		return new BuiltSseClientRequest() {
			@Override
			public SseClient execute(SseExecutionCallback providedSseExecutionCallback) {
				SseClientImpl sseClient = new SseClientImpl(providedSseExecutionCallback, builtNettyClientRequest, httpClientCallbackExecutor);
				sseClient.connect();
				return sseClient;
			}

			@Override
			public SseClient execute() {
				SseClientImpl sseClient = new SseClientImpl(null, builtNettyClientRequest, httpClientCallbackExecutor);
				sseClient.connect();
				return sseClient;
			}

			@Override
			public SseClient build(SseExecutionCallback sseExecutionCallback) {
				return new SseClientImpl(sseExecutionCallback, builtNettyClientRequest, httpClientCallbackExecutor);
			}

			@Override
			public SseClient build() {
				return new SseClientImpl(null, builtNettyClientRequest, httpClientCallbackExecutor);
			}
		};

	}


}
