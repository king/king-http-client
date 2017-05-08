// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClientRequestHeaderBuilder;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class HttpClientRequestHeaderBuilderImpl<T extends HttpClientRequestHeaderBuilder> implements HttpClientRequestHeaderBuilder<T> {
	private final Class<T> implClass;
	protected final NettyHttpClient nettyHttpClient;
	protected final HttpVersion httpVersion;
	protected final HttpMethod httpMethod;
	protected final String uri;

	protected final List<Param> queryParameters = new ArrayList<>();
	protected final List<Param> headerParameters = new ArrayList<>();

	protected final String defaultUserAgent;

	protected int idleTimeoutMillis;
	protected int totalRequestTimeoutMillis;

	protected boolean followRedirects;
	protected boolean acceptCompressedResponse;
	protected boolean keepAlive;
	protected Executor callbackExecutor;


	protected HttpClientRequestHeaderBuilderImpl(Class<T> implClass, NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap, Executor callbackExecutor) {
		this.implClass = implClass;
		this.nettyHttpClient = nettyHttpClient;
		this.httpVersion = httpVersion;
		this.httpMethod = httpMethod;
		this.uri = uri;

		idleTimeoutMillis = confMap.get(ConfKeys.IDLE_TIMEOUT_MILLIS);
		totalRequestTimeoutMillis = confMap.get(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS);
		followRedirects = confMap.get(ConfKeys.HTTP_FOLLOW_REDIRECTS);


		acceptCompressedResponse = confMap.get(ConfKeys.ACCEPT_COMPRESSED_RESPONSE);

		keepAlive = confMap.get(ConfKeys.KEEP_ALIVE);

		defaultUserAgent = confMap.get(ConfKeys.USER_AGENT);

		this.callbackExecutor = callbackExecutor;
	}


	@Override
	public T withHeader(String name, String value) {
		headerParameters.add(new Param(name, value));
		return implClass.cast(this);
	}

	@Override
	public T withHeaders(Map<String, String> headers) {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			withHeader(entry.getKey(), entry.getValue());
		}

		return implClass.cast(this);
	}

	@Override
	public T keepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
		return implClass.cast(this);
	}

	@Override
	public T acceptCompressedResponse(boolean acceptCompressedResponse) {
		this.acceptCompressedResponse = acceptCompressedResponse;
		return implClass.cast(this);
	}

	@Override
	public T withQueryParameter(String name, String value) {
		queryParameters.add(new Param(name, value));
		return implClass.cast(this);
	}

	@Override
	public T withQueryParameters(Map<String, String> parameters) {
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			queryParameters.add(new Param(entry.getKey(), entry.getValue()));
		}
		return implClass.cast(this);
	}

	@Override
	public T idleTimeoutMillis(int idleTimeoutMillis) {
		this.idleTimeoutMillis = idleTimeoutMillis;
		return implClass.cast(this);
	}

	@Override
	public T totalRequestTimeoutMillis(int totalRequestTimeoutMillis) {
		this.totalRequestTimeoutMillis = totalRequestTimeoutMillis;
		return implClass.cast(this);
	}

	@Override
	public T followRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
		return implClass.cast(this);
	}

	@Override
	public T executingOn(Executor executor) {
		this.callbackExecutor = executor;
		return implClass.cast(this);
	}
}
