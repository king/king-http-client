package com.king.platform.net.http.netty.request.multipart;


import io.netty.channel.ChannelProgressivePromise;

public class TotalProgressionTracker {
	private final long contentLength;
	private final ChannelProgressivePromise channelProgressivePromise;

	private long totalProgression;
	private long partialProgression;


	public TotalProgressionTracker(long contentLength, ChannelProgressivePromise channelProgressivePromise) {
		this.contentLength = contentLength;
		this.channelProgressivePromise = channelProgressivePromise;
	}

	public void addPartialProgress(long progress) {
		long delta = progress - partialProgression;
		partialProgression = progress;
		totalProgression += delta;
		channelProgressivePromise.setProgress(totalProgression, contentLength);
	}

	public void partialProgressCompleted() {
		partialProgression = 0;
	}

	public void setSuccess() {
		System.out.println("Tottal write success!");
		channelProgressivePromise.setSuccess();
	}

	public void setFailure(Throwable cause) {
		channelProgressivePromise.setFailure(cause);
	}
}
