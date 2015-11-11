// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public abstract class ChannelHttpCallback implements HttpCallback<WritableByteChannel> {
	private final WritableByteChannel channel;
	private final ProgressCallback progressCallback;

	public ChannelHttpCallback(WritableByteChannel channel) {
		this(channel, null);
	}

	public ChannelHttpCallback(WritableByteChannel channel, ProgressCallback progressCallback) {
		this.channel = channel;
		this.progressCallback = progressCallback;
	}


	@Override
	public ResponseBodyConsumer<WritableByteChannel> newResponseBodyConsumer() {
		return new ResponseBodyStream(channel, new ProgressInvoker(progressCallback));
	}

	public static interface ProgressCallback {
		void onProgress(int percentageCompleted, long readLength, long contentLength);
	}

	private static class ResponseBodyStream implements ResponseBodyConsumer<WritableByteChannel> {
		private final WritableByteChannel channel;

		private final ProgressInvoker progressInvoker;

		public ResponseBodyStream(WritableByteChannel channel, ProgressInvoker progressInvoker) {
			this.channel = channel;
			this.progressInvoker = progressInvoker;
		}

		@Override
		public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
			progressInvoker.setContentLength(contentLength);
		}

		@Override
		public void onReceivedContentPart(ByteBuffer buffer) throws Exception {

			int remaining = buffer.remaining();
			channel.write(buffer);

			progressInvoker.updateReceivedContent(remaining);
		}

		@Override
		public void onCompletedBody() throws Exception {

			channel.close();
		}

		@Override
		public WritableByteChannel getBody() {
			return channel;
		}
	}

	private static class ProgressInvoker {
		private final ProgressCallback progressCallback;
		private long contentLength;
		private long readLength;

		public ProgressInvoker(ProgressCallback progressCallback) {
			this.progressCallback = progressCallback;
		}

		public long getContentLength() {
			return contentLength;
		}

		public void setContentLength(long contentLength) {
			this.contentLength = contentLength;
		}

		public long getReadLength() {
			return readLength;
		}


		public void updateReceivedContent(int remaining) {
			if (progressCallback != null) {
				int prev = (int) (((float) readLength / (float) contentLength) * 100f);

				readLength += remaining;

				int current = (int) (((float) readLength / (float) contentLength) * 100f);

				if (current != prev) {
					progressCallback.onProgress(current, readLength, contentLength);
				}

			}
		}

	}
}
