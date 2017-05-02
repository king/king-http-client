// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class InputStreamHttpBody implements HttpBody {
	private final InputStream inputStream;
	private final String contentType;
	private final Charset characterEncoding;

	public InputStreamHttpBody(InputStream inputStream, String contentType, Charset characterEncoding) {
		this.inputStream = inputStream;
		this.contentType = contentType;
		this.characterEncoding = characterEncoding;
	}

	@Override
	public long getContentLength() {
		return -1L;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public Charset getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public ChannelFuture writeContent(ChannelHandlerContext ctx) throws IOException {
		final InputStream is = inputStream;

		Channel channel = ctx.channel();
		ChannelFuture channelFuture = channel.write(new ChunkedStream(inputStream), channel.newProgressivePromise());
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				is.close();
			}
		});
		return channelFuture;

	}
}
