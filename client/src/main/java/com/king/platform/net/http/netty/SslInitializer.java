// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;

public class SslInitializer extends ChannelOutboundHandlerAdapter {
	private final SSLFactory sslFactory;
	public static final String NAME = "SslHandler";
	private final long handshakeTimeout;

	public SslInitializer(SSLFactory sslFactory, long handshakeTimeout) {
		this.sslFactory = sslFactory;
		this.handshakeTimeout = handshakeTimeout;
	}

	@Override
	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		InetSocketAddress remoteInetSocketAddress = (InetSocketAddress) remoteAddress;
		String peerHost = remoteInetSocketAddress.getHostString();
		int peerPort = remoteInetSocketAddress.getPort();

		SslHandler sslHandler = createSslHandler(peerHost, peerPort);

		ctx.pipeline().replace(NAME, NAME, sslHandler);

		ctx.connect(remoteAddress, localAddress, promise);
	}

	public SslHandler createSslHandler(String peerHost, int peerPort) throws IOException, GeneralSecurityException {
		SSLEngine sslEngine = sslFactory.newSSLEngine(peerHost, peerPort);
		SslHandler sslHandler = new SslHandler(sslEngine);
		if (handshakeTimeout > 0) {
			sslHandler.setHandshakeTimeoutMillis(handshakeTimeout);
		}
		return sslHandler;
	}

}
