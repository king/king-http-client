package com.king.platform.net.http.netty.request.multipart;


import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;

public class TotalProgressiveFutureListener implements ChannelProgressiveFutureListener {
	private final TotalProgressionTracker totalProgressionTracker;

	public TotalProgressiveFutureListener(TotalProgressionTracker totalProgressionTracker) {
		this.totalProgressionTracker = totalProgressionTracker;
	}

	@Override
	public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
		totalProgressionTracker.addPartialProgress(progress);
	}

	@Override
	public void operationComplete(ChannelProgressiveFuture future) throws Exception {
		totalProgressionTracker.partialProgressCompleted();
	}
}
