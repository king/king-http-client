// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltClientRequest;
import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClientRequestWithBodyBuilder;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpClientRequestWithBodyBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientRequestWithBodyBuilder> implements HttpClientRequestWithBodyBuilder {

	private RequestBodyBuilder requestBodyBuilder;
	private String contentType;
	private Charset bodyCharset;

	public HttpClientRequestWithBodyBuilderImpl(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {
		super(HttpClientRequestWithBodyBuilder.class, nettyHttpClient, httpVersion, httpMethod, uri, confMap);

		bodyCharset = confMap.get(ConfKeys.REQUEST_BODY_CHARSET);

	}


	@Override
	public HttpClientRequestWithBodyBuilder content(byte[] content) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type " + requestBodyBuilder.getClass());
		}
		requestBodyBuilder = new ByteArrayHttpBodyBuilder(content);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(File file) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new FileHttpBodyBuilder(file);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(HttpBody httpBody) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
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
	public HttpClientRequestWithBodyBuilder bodyCharset(Charset charset) {
		this.bodyCharset = charset;
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
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		if (!(requestBodyBuilder instanceof FormParameterBodyBuilder)) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(InputStream inputStream) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new InputStreamHttpBodyBuilder(inputStream);

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
