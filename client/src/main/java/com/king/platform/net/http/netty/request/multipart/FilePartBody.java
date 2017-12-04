package com.king.platform.net.http.netty.request.multipart;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FilePartBody extends AbstractPartBody {
	private final File file;
	private final long contentLength;

	public FilePartBody(File file) {
		this.file = file;
		contentLength = file.length();
	}


	@Override
	public void writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException {

		ChannelFuture future;

		if (isSecure) {
			future = writeChunkedContent(ctx);
		} else {
			future = writeStreamedContent(ctx);
		}

		future.addListener(new TotalProgressiveFutureListener(totalProgressionTracker));

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

	@Override
	public long getContentLength() {
		return contentLength;
	}
}
