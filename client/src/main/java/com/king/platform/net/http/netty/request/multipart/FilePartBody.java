package com.king.platform.net.http.netty.request.multipart;


import io.netty.channel.Channel;
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
		if (isSecure) {
			writeChunkedContent(ctx, new TotalProgressiveFutureListener(totalProgressionTracker));
		} else {
			writeStreamedContent(ctx, new TotalProgressiveFutureListener(totalProgressionTracker));
		}
	}

	private void writeStreamedContent(ChannelHandlerContext ctx, TotalProgressiveFutureListener totalProgressiveFutureListener) {
		Channel channel = ctx.channel();
		channel.writeAndFlush(new DefaultFileRegion(file, 0, file.length()), channel.newProgressivePromise().addListener(totalProgressiveFutureListener));
	}

	private void writeChunkedContent(ChannelHandlerContext ctx, TotalProgressiveFutureListener totalProgressiveFutureListener) throws IOException {
		Channel channel = ctx.channel();
		FileChannel fileChannel = new FileInputStream(file).getChannel();
		long length = file.length();
		channel.writeAndFlush(new ChunkedNioFile(fileChannel, 0, length, 1024 * 8), channel.newProgressivePromise()
			.addListener(totalProgressiveFutureListener));
	}

	@Override
	public long getContentLength() {
		return contentLength;
	}
}
