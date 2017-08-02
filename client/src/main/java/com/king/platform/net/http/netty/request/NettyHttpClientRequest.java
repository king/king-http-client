// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;


import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.util.UriUtil;
import io.netty.handler.codec.http.*;

public class NettyHttpClientRequest<T> {
	private final ServerInfo serverInfo;
	private final HttpRequest nettyRequest;
	private final HttpHeaders nettyHeaders;
	private final HttpBody httpBody;


	public NettyHttpClientRequest(ServerInfo serverInfo, HttpRequest nettyRequest, HttpBody httpBody) {
		this.serverInfo = serverInfo;
		this.nettyRequest = nettyRequest;
		this.httpBody = httpBody;

		this.nettyHeaders = nettyRequest.headers();


	}


	public NettyHttpClientRequest createRedirectRequest(ServerInfo redirectServerInfo, String uri) {
		NettyHttpClientRequest redirectRequest = new NettyHttpClientRequest(redirectServerInfo, nettyRequest, httpBody);
		String relativePath = UriUtil.getRelativeUri(uri);
		redirectRequest.nettyRequest.setUri(relativePath);

		return redirectRequest;
	}


	public boolean isDontWriteBodyBecauseExpectContinue() {
		String expectHeader = nettyHeaders.get(HttpHeaderNames.EXPECT);
		return expectHeader != null && HttpHeaderValues.CONTINUE.contentEqualsIgnoreCase(expectHeader);
	}

	public void setKeepAlive(boolean keepAlive) {
		HttpUtil.setKeepAlive(nettyRequest, keepAlive);
	}


	public ServerInfo getServerInfo() {
		return serverInfo;
	}

	public HttpRequest getNettyRequest() {
		return nettyRequest;
	}

	public HttpHeaders getNettyHeaders() {
		return nettyHeaders;
	}

	public HttpBody getHttpBody() {
		return httpBody;
	}

	public String getHost() {
		return serverInfo.getHost();
	}

	public int getPort() {
		return serverInfo.getPort();
	}


	@Override
	public String toString() {
		return "NettyHttpClientRequest{" +
			", serverInfo=" + serverInfo +
			", httpBody=" + httpBody +
			'}';
	}

}
