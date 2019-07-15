package com.king.platform.net.http.netty;


public class WebSocketConf {
	private int maxFrameSize;
	private boolean aggregateFrames;
	private int maxAggregateBufferSize;
	private boolean splitLargeFrames;

	public WebSocketConf(int maxFrameSize, boolean aggregateFrames, int maxAggregateBufferSize, boolean splitLargeFrames) {
		this.maxFrameSize = maxFrameSize;
		this.aggregateFrames = aggregateFrames;
		this.maxAggregateBufferSize = maxAggregateBufferSize;
		this.splitLargeFrames = splitLargeFrames;
	}

	public int getMaxFrameSize() {
		return maxFrameSize;
	}

	public boolean isAggregateFrames() {
		return aggregateFrames;
	}

	public int getMaxAggregateBufferSize() {
		return maxAggregateBufferSize;
	}

	public boolean isSplitLargeFrames() {
		return splitLargeFrames;
	}
}
