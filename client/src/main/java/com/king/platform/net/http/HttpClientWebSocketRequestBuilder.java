package com.king.platform.net.http;

import java.time.Duration;

public interface HttpClientWebSocketRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientWebSocketRequestBuilder> {

	/**
	 * Specify what sub-protocol this client desires
	 *
	 * @param subProtocols the sub protocol
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder subProtocols(String subProtocols);

	/**
	 * Specify if the client should automatically send ping to the server to keep the connection alive.
	 *
	 * @param duration the duration between the pings
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder pingEvery(Duration duration);

	/**
	 * Specify if the client should automatically respond with pongs when it receives an ping from the server.
	 *
	 * @param autoPong true if it should respond.
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder autoPong(boolean autoPong);

	/**
	 * Specify if the client should automatically respond with the close frame to the server before closing the connection.
	 *
	 * @param autoCloseFrame true if it should respond.
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder autoCloseFrame(boolean autoCloseFrame);

	/**
	 * Specify the max allowed frame size (for both incoming and outgoing frames).
	 * This overrides {@link ConfKeys#WEB_SOCKET_MAX_FRAME_SIZE}
	 * @param maxFrameSize the max frame size
	 * @return this builder
	 */
	@Deprecated
	HttpClientWebSocketRequestBuilder maxFrameSize(int maxFrameSize);

	/**
	 * Specify if the client should automatically aggregate incoming continuation frames.
	 * This overrides {@link ConfKeys#WEB_SOCKET_AGGREGATE_FRAMES}
	 * @param aggregateFrames flag to indicate if frames should be aggregated
	 * @return this builder
	 */
	@Deprecated
	HttpClientWebSocketRequestBuilder aggregateFrames(boolean aggregateFrames);


	/**
	 * Specify if the client should automatically split outgoing frames if they are larger then {@link #maxFrameSize(int)}
	 * This overrides the {@link ConfKeys#WEB_SOCKET_SPLIT_FRAMES}.
	 * @param splitLargeFrames flag to indicate that the outgoing frame should be split
	 * @return this builder
	 */
	@Deprecated
	HttpClientWebSocketRequestBuilder splitLargeFrames(boolean splitLargeFrames);

	/**
	 * Specify the max size that the client will aggregate continuation frames. This is only used if {@link #aggregateFrames(boolean)} or {@link ConfKeys#WEB_SOCKET_AGGREGATE_FRAMES} is true.
	 * This overrides {@link ConfKeys#WEB_SOCKET_MAX_AGGREGATE_BUFFER_SIZE}.
	 * @param maxAggregateBufferSize the max size of the buffer used for aggregation
	 * @return this builder
	 */
	HttpClientWebSocketRequestBuilder maxAggregateBufferSize(int maxAggregateBufferSize);

	HttpClientWebSocketRequestBuilder maxOutgoingFrameSize(int maxOutgoingFrameSize);

	HttpClientWebSocketRequestBuilder maxIncomingFrameSize(int maxIncomingFrameSize);


	/**
	 * Build the reusable web-socket request
	 * @return the built request
	 */
	BuiltWebSocketRequest build();

}
