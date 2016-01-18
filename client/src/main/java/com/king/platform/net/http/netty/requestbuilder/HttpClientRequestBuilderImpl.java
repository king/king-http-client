// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltClientRequest;
import com.king.platform.net.http.HttpClientRequestBuilder;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class HttpClientRequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientRequestBuilder> implements HttpClientRequestBuilder {

	public HttpClientRequestBuilderImpl(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {
		super(nettyHttpClient, httpVersion, httpMethod, uri, confMap);
	}

	@Override
	public BuiltClientRequest build() {
		return new BuiltNettyClientRequest(nettyHttpClient, httpVersion, httpMethod, uri, defaultUserAgent, idleTimeoutMillis, totalRequestTimeoutMillis,
			followRedirects, acceptCompressedResponse, keepAlive, null, null, null, queryParameters, headerParameters);
	}
}
