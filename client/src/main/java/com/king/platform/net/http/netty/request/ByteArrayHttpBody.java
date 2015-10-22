// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;

public class ByteArrayHttpBody implements HttpBody {
	private final byte[] content;
	private final String contentType;

	public ByteArrayHttpBody(byte[] content, String contentType) {
		this.content = Arrays.copyOf(content, content.length);
		this.contentType = contentType;
	}

	@Override
	public ChannelFuture writeContent(ChannelHandlerContext ctx) {
		ByteBuf byteBuf = ctx.alloc().buffer(content.length).writeBytes(content);
		return ctx.write(byteBuf, ctx.newProgressivePromise());
	}

	@Override
	public long getContentLength() {
		return content.length;
	}

	@Override
	public String getContentType() {
		return contentType;
	}
}
