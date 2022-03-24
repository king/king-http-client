// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.HttpClientCaller;
import com.king.platform.net.http.netty.ServerInfoValidator;
import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class HttpClientRequestWithBodyBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientRequestWithBodyBuilder> implements HttpClientRequestWithBodyBuilder {

	private RequestBodyBuilder requestBodyBuilder;
	private String contentType;
	private Charset bodyCharset;

	public HttpClientRequestWithBodyBuilderImpl(HttpClientCaller httpClientCaller, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap,
												Executor callbackExecutor) {
		super(HttpClientRequestWithBodyBuilder.class, httpClientCaller, httpVersion, httpMethod, uri, confMap, callbackExecutor);

		bodyCharset = confMap.get(ConfKeys.REQUEST_BODY_CHARSET);

	}


	@Override
	public HttpClientRequestWithBodyBuilder content(byte[] content) {
		requireNonNull(content, "Content can't be null");

		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type " + requestBodyBuilder.getClass());
		}
		requestBodyBuilder = new ByteArrayHttpBodyBuilder(content);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(File file) {
		requireNonNull(file, "File can't be null");

		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new FileHttpBodyBuilder(file);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(HttpBody httpBody) {
		requireNonNull(httpBody, "HttpBody can't be null");
		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new CustomHttpBodyBuilder(httpBody);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder contentType(String contentType) {
		this.contentType = requireNonNull(contentType, "Content-Type can't be null");
		return this;
	}


	@Override
	public HttpClientRequestWithBodyBuilder bodyCharset(Charset charset) {
		this.bodyCharset = requireNonNull(charset, "Charset can't be null");
		return this;
	}


	@Override
	public HttpClientRequestWithBodyBuilder addFormParameter(String name, String value) {
		requireNonNull(name, "name");
		requireNonNull(value, "value");

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
		requireNonNull(inputStream, "InputStream can't be null");

		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new InputStreamHttpBodyBuilder(inputStream);

		return this;
	}

	@Override
	public HttpClientRequestWithBodyBuilder content(BuiltMultiPart builtMultiPart) {
		requireNonNull(builtMultiPart, "Built multi part can't be null");

		if (requestBodyBuilder != null) {
			throw new RuntimeException("Already defined request body as type  " + requestBodyBuilder.getClass());
		}

		requestBodyBuilder = new MultiPartHttpBodyBuilder(builtMultiPart);

		return this;
	}

	@Override
	public <T> BuiltClientRequestWithBody<T> build(Supplier<ResponseBodyConsumer<T>> responseBodyConsumer) {
		requireNonNull(responseBodyConsumer, "Supplier of ResponseBodyConsumer can't be null");

		RequestBodyBuilder immutableBodyBuilder = requestBodyBuilder;
		if (requestBodyBuilder instanceof FormParameterBodyBuilder) {
			immutableBodyBuilder = new FormParameterBodyBuilder((FormParameterBodyBuilder)requestBodyBuilder);
		}

		return new BuiltNettyClientRequest<T>(httpClientCaller, httpVersion, httpMethod, uri, defaultUserAgent, idleTimeoutMillis, totalRequestTimeoutMillis, followRedirects,
			acceptCompressedResponse, keepAlive, keepAliveTimeoutMillis, automaticallyDecompressResponse, immutableBodyBuilder, contentType, bodyCharset, queryParameters, headerParameters, callbackExecutor, responseBodyConsumer, null, ServerInfoValidator.HTTP);
	}


	@Override
	public BuiltClientRequestWithBody<String> build() {
		return build(StringResponseBody::new);
	}
}
