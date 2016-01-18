// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltClientRequest;
import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClientRequestBuilder;
import com.king.platform.net.http.HttpClientRequestWithBodyBuilder;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientRequestBuilderImpl implements HttpClientRequestBuilder, HttpClientRequestWithBodyBuilder {
	private final NettyHttpClient nettyHttpClient;
	private final HttpVersion httpVersion;
	private final HttpMethod httpMethod;
	private final String uri;

	private final List<Param> queryParameters = new ArrayList<>();
	private final List<Param> headerParameters = new ArrayList<>();

	private final String defaultUserAgent;


	private int idleTimeoutMillis;
	private int totalRequestTimeoutMillis;

	private boolean followRedirects;
	private boolean acceptCompressedResponse;
	private boolean keepAlive;

	private RequestBodyBuilder requestBodyBuilder;
	private String contentType;
	private Charset bodyCharset;


	public HttpClientRequestBuilderImpl(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {


		this.nettyHttpClient = nettyHttpClient;
		this.httpVersion = httpVersion;
		this.httpMethod = httpMethod;
		this.uri = uri;

		idleTimeoutMillis = confMap.get(ConfKeys.IDLE_TIMEOUT_MILLIS);
		totalRequestTimeoutMillis = confMap.get(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS);
		followRedirects = confMap.get(ConfKeys.HTTP_FOLLOW_REDIRECTS);


		acceptCompressedResponse = confMap.get(ConfKeys.ACCEPT_COMPRESSED_RESPONSE);

		keepAlive = confMap.get(ConfKeys.KEEP_ALIVE);

		bodyCharset = confMap.get(ConfKeys.REQUEST_BODY_CHARSET);

		defaultUserAgent = confMap.get(ConfKeys.USER_AGENT);
	}


	@Override
	public HttpClientRequestWithBodyBuilder withHeader(String name, String value) {
		headerParameters.add(new Param(name, value));
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(byte[] content) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}
		requestBodyBuilder = new ByteArrayHttpBodyBuilder(content);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(File file) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new FileHttpBodyBuilder(file);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(HttpBody httpBody) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new CustomHttpBodyBuilder(httpBody);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder keepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder bodyCharset(Charset charset) {
		this.bodyCharset = charset;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder acceptCompressedResponse(boolean acceptCompressedResponse) {
		this.acceptCompressedResponse = acceptCompressedResponse;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder withQueryParameter(String name, String value) {
		queryParameters.add(new Param(name, value));
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder addFormParameter(String name, String value) {
		validateRequestBuilderStat(FormParameterBodyBuilder.class);

		if (requestBodyBuilder == null) {
			requestBodyBuilder = new FormParameterBodyBuilder();
		}

		((FormParameterBodyBuilder) requestBodyBuilder).addParameter(name, value);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder addFormParameters(Map<String, String> parameters) {
		validateRequestBuilderStat(FormParameterBodyBuilder.class);

		if (requestBodyBuilder == null) {
			requestBodyBuilder = new FormParameterBodyBuilder();
		}

		((FormParameterBodyBuilder) requestBodyBuilder).addParameters(parameters);

		return this;
	}

	private void validateRequestBuilderStat(Class... allowedTypes) {
		if (requestBodyBuilder == null) {
			return;
		}

		if (allowedTypes == null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		if (!(requestBodyBuilder instanceof FormParameterBodyBuilder)) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(InputStream inputStream) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new InputStreamHttpBodyBuilder(inputStream);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder idleTimeoutMillis(int idleTimeoutMillis) {
		this.idleTimeoutMillis = idleTimeoutMillis;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder totalRequestTimeoutMillis(int totalRequestTimeoutMillis) {
		this.totalRequestTimeoutMillis = totalRequestTimeoutMillis;
		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder followRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
		return this;
	}

	@Override
	public BuiltClientRequest build() {
		RequestBodyBuilder immutableBodyBuilder = requestBodyBuilder;
		if (requestBodyBuilder instanceof FormParameterBodyBuilder) {
			immutableBodyBuilder = new FormParameterBodyBuilder((FormParameterBodyBuilder)requestBodyBuilder);
		}

		return new BuiltNettyClientRequest(nettyHttpClient, httpVersion, httpMethod, uri, defaultUserAgent, idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects,
			acceptCompressedResponse, keepAlive, immutableBodyBuilder, contentType, bodyCharset, queryParameters, headerParameters);
	}
}
