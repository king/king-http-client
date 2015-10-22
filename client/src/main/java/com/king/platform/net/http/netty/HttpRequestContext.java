// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.metric.TimeStampRecorder;
import com.king.platform.net.http.netty.request.NettyHttpClientRequest;
import com.king.platform.net.http.netty.response.NettyHttpClientResponse;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;

public class HttpRequestContext<T> {
	public static final AttributeKey<HttpRequestContext> HTTP_REQUEST_ATTRIBUTE_KEY = AttributeKey.valueOf("__HttpRequestContext");

	private final NettyHttpClientRequest<T> nettyHttpClientRequest;

	private final int idleTimeoutMillis;
	private final int totalRequestTimeoutMillis;
	private final boolean followRedirects;
	private final boolean keepAlive;
	private final TimeStampRecorder timeStampRecorder;
	private final ResponseBodyConsumer<T> responseBodyConsumer;
	private final RequestEventBus requestEventBus;


	private NettyHttpClientResponse<T> nettyHttpClientResponse;
	private int redirectionCount;
	private Channel attachedChannel;


	public HttpRequestContext(NettyHttpClientRequest<T> nettyHttpClientRequest, RequestEventBus requestEventBus, ResponseBodyConsumer<T> responseBodyConsumer,
							  int idleTimeoutMillis, int totalRequestTimeoutMillis, boolean followRedirects, boolean keepAlive, TimeStampRecorder
								  timeStampRecorder) {
		this.nettyHttpClientRequest = nettyHttpClientRequest;
		this.requestEventBus = requestEventBus;
		this.responseBodyConsumer = responseBodyConsumer;
		this.idleTimeoutMillis = idleTimeoutMillis;
		this.totalRequestTimeoutMillis = totalRequestTimeoutMillis;
		this.followRedirects = followRedirects;
		this.keepAlive = keepAlive;
		this.timeStampRecorder = timeStampRecorder;
		timeStampRecorder.recordCreatedRequest();
	}

	public HttpRequestContext createRedirectRequest(ServerInfo redirectServerInfo, String redirectLocation) {
		NettyHttpClientRequest redirectRequest = nettyHttpClientRequest.createRedirectRequest(redirectServerInfo, redirectLocation);
		HttpRequestContext httpRequestContext = new HttpRequestContext(redirectRequest, requestEventBus.createRequestEventBus(), responseBodyConsumer,
			idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects, keepAlive, timeStampRecorder);
		httpRequestContext.redirectionCount = this.redirectionCount + 1;

		nettyHttpClientRequest.getNettyHeaders().set(HttpHeaders.Names.HOST, redirectServerInfo.getHost() + ":" + redirectServerInfo.getPort());
		nettyHttpClientRequest.setKeepAlive(keepAlive);
		return httpRequestContext;
	}

	public ServerInfo getServerInfo() {
		return nettyHttpClientRequest.getServerInfo();
	}


	public NettyHttpClientRequest<T> getNettyHttpClientRequest() {
		return nettyHttpClientRequest;
	}


	public int getIdleTimeoutMillis() {
		return idleTimeoutMillis;
	}

	public int getTotalRequestTimeoutMillis() {
		return totalRequestTimeoutMillis;
	}

	public boolean isFollowRedirects() {
		return followRedirects;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public ResponseBodyConsumer<T> getResponseBodyConsumer() {
		return responseBodyConsumer;
	}

	public NettyHttpClientResponse<T> getNettyHttpClientResponse() {
		return nettyHttpClientResponse;
	}

	public void setNettyHttpClientResponse(NettyHttpClientResponse nettyHttpClientResponse) {
		this.nettyHttpClientResponse = nettyHttpClientResponse;
	}

	public int getRedirectionCount() {
		return redirectionCount;
	}

	public RequestEventBus getRequestEventBus() {
		return requestEventBus;
	}

	public void attachedToChannel(Channel channel) {
		this.attachedChannel = channel;
	}

	public Channel getAndDetachChannel() {
		Channel channel = attachedChannel;
		attachedChannel = null;
		return channel;
	}

	public TimeStampRecorder getTimeRecorder() {
		return timeStampRecorder;
	}
}
