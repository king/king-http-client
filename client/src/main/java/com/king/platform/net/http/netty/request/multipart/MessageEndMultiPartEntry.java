package com.king.platform.net.http.netty.request.multipart;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class MessageEndMultiPartEntry extends MultiPartEntry {

	private final int contentLength;

	public MessageEndMultiPartEntry(byte[] boundary) {
		super(null, boundary);
		contentLength = EXTRA_BYTES.length + boundary.length + EXTRA_BYTES.length + CRLF_BYTES.length;
	}

	@Override
	public long getContentLength() {
		return contentLength;
	}

	@Override
	public ChannelFuture writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException {
		ByteBuf lastContentBuffer = ctx.alloc()
			.buffer(contentLength)
			.writeBytes(EXTRA_BYTES)
			.writeBytes(boundary)
			.writeBytes(EXTRA_BYTES)
			.writeBytes(CRLF_BYTES);

		ChannelFuture future = ctx.writeAndFlush(lastContentBuffer, ctx.newProgressivePromise());
		future.addListener(new TotalProgressiveFutureListener(totalProgressionTracker));
		return future;
	}
}
