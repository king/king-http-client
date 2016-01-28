package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltSSEClientRequest;
import com.king.platform.net.http.HttpClientSSERequestBuilder;
import com.king.platform.net.http.netty.ConfMap;
import com.king.platform.net.http.netty.NettyHttpClient;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class HttpClientSSERequestBuilderImpl extends HttpClientRequestHeaderBuilderImpl<HttpClientSSERequestBuilder> implements HttpClientSSERequestBuilder {

	public HttpClientSSERequestBuilderImpl(NettyHttpClient nettyHttpClient, HttpVersion httpVersion, HttpMethod httpMethod, String uri, ConfMap confMap) {
		super(HttpClientSSERequestBuilder.class, nettyHttpClient, httpVersion, httpMethod, uri, confMap);
	}

	@Override
	public BuiltSSEClientRequest build() {
		return null;
	}
}
