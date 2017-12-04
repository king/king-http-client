// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FileHttpBody implements HttpBody {
	private final File file;
	private final String contentType;
	private final long contentLength;
	private Charset characterEncoding;

	public FileHttpBody(File file, String contentType, Charset characterEncoding) {
		this.file = file;
		this.contentType = contentType;
		this.contentLength = file.length();
		this.characterEncoding = characterEncoding;
	}


	@Override
	public long getContentLength() {
		return contentLength;
	}

	@Override
	public String getContentType() {
		if (contentType != null) {
			return contentType;
		}
		return "application/binary";
	}

	@Override
	public Charset getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public ChannelFuture writeContent(ChannelHandlerContext ctx, boolean isSecure) throws IOException {
		if (isSecure) {
			return writeChunkedContent(ctx);
		} else {
			return writeStreamedContent(ctx);
		}
	}

	private ChannelFuture writeStreamedContent(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		return channel.write(new DefaultFileRegion(file, 0, file.length()), channel.newProgressivePromise());
	}

	private ChannelFuture writeChunkedContent(ChannelHandlerContext ctx) throws IOException {
		Channel channel = ctx.channel();
		FileChannel fileChannel = new FileInputStream(file).getChannel();
		long length = file.length();
		return channel.write(new ChunkedNioFile(fileChannel, 0, length, 1024 * 8), channel.newProgressivePromise());
	}

}
