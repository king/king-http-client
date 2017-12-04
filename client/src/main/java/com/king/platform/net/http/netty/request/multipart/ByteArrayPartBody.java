package com.king.platform.net.http.netty.request.multipart;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class ByteArrayPartBody extends AbstractPartBody {
	private byte[] content;

	public ByteArrayPartBody(byte[] content) {
		this.content = content;
	}

	@Override
	public void writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException {
		ByteBuf byteBuf = ctx.alloc().buffer(content.length).writeBytes(content);
		ctx.writeAndFlush(byteBuf, ctx.newProgressivePromise().addListener(new TotalProgressiveFutureListener(totalProgressionTracker)));
	}

	@Override
	public long getContentLength() {
		return content.length;
	}
}
