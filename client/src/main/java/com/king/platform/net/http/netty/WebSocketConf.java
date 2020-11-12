package com.king.platform.net.http.netty;


public class WebSocketConf {
	@Deprecated
	private final int maxFrameSize;
	@Deprecated
	private final boolean aggregateFrames;
	@Deprecated
	private final boolean splitLargeFrames;


	private final int maxAggregateBufferSize;
	private final int maxIncomingFrameSize;
	private final int maxOutgoingFrameSize;

	public WebSocketConf(int maxFrameSize, boolean aggregateFrames, int maxAggregateBufferSize, boolean splitLargeFrames, int maxIncomingFrameSize, int maxOutgoingFrameSize) {
		this.maxFrameSize = maxFrameSize;
		this.aggregateFrames = aggregateFrames;
		this.maxAggregateBufferSize = maxAggregateBufferSize;
		this.splitLargeFrames = splitLargeFrames;
		this.maxIncomingFrameSize = maxIncomingFrameSize;
		this.maxOutgoingFrameSize = maxOutgoingFrameSize;
	}

	@Deprecated
	public int getMaxFrameSize() {
		return maxFrameSize;
	}

	@Deprecated
	public boolean isSplitLargeFrames() {
		return splitLargeFrames;
	}

	@Deprecated
	public boolean isAggregateFrames() {
		return aggregateFrames;
	}

	public int getMaxAggregateBufferSize() {
		return maxAggregateBufferSize;
	}


	public int getMaxIncomingFrameSize() {
		return maxIncomingFrameSize;
	}

	public int getMaxOutgoingFrameSize() {
		return maxOutgoingFrameSize;
	}
}
