// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.*;
import com.king.platform.net.http.netty.request.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientRequestBuilder implements HttpClientRequest, HttpClientRequestWithBody {
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


	public HttpClientRequestBuilder(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {


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
	public HttpClientRequestWithBody withHeader(String name, String value) {
		headerParameters.add(new Param(name, value));
		return this;
	}

	@Override
	public HttpClientRequestWithBody content(byte[] content) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}
		requestBodyBuilder = new ByteArrayHttpBodyBuilder(content);

		return this;
	}

	@Override
	public HttpClientRequestWithBody content(File file) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new FileHttpBodyBuilder(file);

		return this;
	}

	@Override
	public HttpClientRequestWithBody content(HttpBody httpBody) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new CustomHttpBodyBuilder(httpBody);

		return this;
	}

	@Override
	public HttpClientRequestWithBody contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public HttpClientRequestWithBody keepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
		return this;
	}

	@Override
	public HttpClientRequestWithBody bodyCharset(Charset charset) {
		this.bodyCharset = charset;
		return this;
	}

	@Override
	public HttpClientRequestWithBody acceptCompressedResponse(boolean acceptCompressedResponse) {
		this.acceptCompressedResponse = acceptCompressedResponse;
		return this;
	}

	@Override
	public HttpClientRequestWithBody withQueryParameter(String name, String value) {
		queryParameters.add(new Param(name, value));
		return this;
	}

	@Override
	public HttpClientRequestWithBody addFormParameter(String name, String value) {
		validateRequestBuilderStat(FormParameterBodyBuilder.class);

		if (requestBodyBuilder == null) {
			requestBodyBuilder = new FormParameterBodyBuilder();
		}

		((FormParameterBodyBuilder) requestBodyBuilder).addParameter(name, value);

		return this;
	}

	@Override
	public HttpClientRequestWithBody addFormParameters(Map<String, String> parameters) {
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
	public HttpClientRequestWithBody content(InputStream inputStream) {
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as " + requestBodyBuilder.getName());
		}

		requestBodyBuilder = new InputStreamHttpBodyBuilder(inputStream);

		return this;
	}

	@Override
	public HttpClientRequestWithBody idleTimeoutMillis(int idleTimeoutMillis) {
		this.idleTimeoutMillis = idleTimeoutMillis;
		return this;
	}

	@Override
	public HttpClientRequestWithBody totalRequestTimeoutMillis(int totalRequestTimeoutMillis) {
		this.totalRequestTimeoutMillis = totalRequestTimeoutMillis;
		return this;
	}

	@Override
	public HttpClientRequestWithBody followRedirects(boolean followRedirects) {
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
