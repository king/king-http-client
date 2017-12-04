package com.king.platform.net.http.netty.request.multipart;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedStream;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamPartBody extends AbstractPartBody {
	private final InputStream inputStream;

	public InputStreamPartBody(InputStream inputStream) {
		this.inputStream = inputStream;
	}


	@Override
	public void writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException {
		final InputStream is = inputStream;

		Channel channel = ctx.channel();
		ChannelFuture channelFuture = channel.write(new ChunkedStream(inputStream), channel.newProgressivePromise());

		channelFuture.addListener((ChannelFutureListener) future -> is.close());
		channelFuture.addListener(new TotalProgressiveFutureListener(totalProgressionTracker));

	}

	@Override
	public long getContentLength() {
		return -1;
	}
}
