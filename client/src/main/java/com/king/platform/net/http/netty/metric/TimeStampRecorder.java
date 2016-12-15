// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;


import com.king.platform.net.http.netty.util.TimeProvider;

public class TimeStampRecorder implements RecordedTimeStamps {
	private final TimeProvider timeProvider;
	private long createdRequest;
	private long startWriteHeaders;
	private long completedWriteHeaders;
	private long startWriteBody;
	private long completedWriteBody;
	private long completedWriteLastBody;
	private long readResponseHttpHeaders;
	private long responseBodyStart;
	private long responseBodyCompleted;

	public TimeStampRecorder(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	public void recordCreatedRequest() {
		createdRequest = timeProvider.currentTimeInMillis();
	}

	public void startWriteHeaders() {
		startWriteHeaders = timeProvider.currentTimeInMillis();
	}

	public void completedWriteHeaders() {
		completedWriteHeaders = timeProvider.currentTimeInMillis();
	}

	public void startWriteBody() {
		startWriteBody = timeProvider.currentTimeInMillis();
	}

	public void completedWriteBody() {
		completedWriteBody = timeProvider.currentTimeInMillis();
	}

	public void completedWriteLastBody() {
		completedWriteLastBody = timeProvider.currentTimeInMillis();
	}

	public void readResponseHttpHeaders() {
		readResponseHttpHeaders = timeProvider.currentTimeInMillis();
	}

	public void responseBodyStart() {
		responseBodyStart = timeProvider.currentTimeInMillis();
	}

	public void responseBodyCompleted() {
		responseBodyCompleted = timeProvider.currentTimeInMillis();
	}

	@Override
	public long getCreatedRequest() {
		return createdRequest;
	}

	@Override
	public long getStartWriteHeaders() {
		return startWriteHeaders;
	}

	@Override
	public long getCompletedWriteHeaders() {
		return completedWriteHeaders;
	}

	@Override
	public long getStartWriteBody() {
		return startWriteBody;
	}

	@Override
	public long getCompletedWriteBody() {
		return completedWriteBody;
	}

	@Override
	public long getCompletedWriteLastBody() {
		return completedWriteLastBody;
	}

	@Override
	public long getReadResponseHttpHeaders() {
		return readResponseHttpHeaders;
	}

	@Override
	public long getResponseBodyStart() {
		return responseBodyStart;
	}

	@Override
	public long getResponseBodyCompleted() {
		return responseBodyCompleted;
	}


	@Override
	public long getCompleteRequestTime() {
		return responseBodyCompleted - createdRequest;
	}

	@Override
	public long getRequestTime() {
		return completedWriteLastBody - startWriteHeaders;
	}

	@Override
	public long getResponseTime() {
		return responseBodyCompleted - readResponseHttpHeaders;
	}

	@Override
	public long getServerProcessTime() {
		return readResponseHttpHeaders - completedWriteLastBody;
	}

	@Override
	public String toString() {
		return "TimeStampRecorder{" + "createdRequest=" + createdRequest + ", startWriteHeaders=" + startWriteHeaders + ", completedWriteHeaders=" +
			completedWriteHeaders + ", startWriteBody=" + startWriteBody + ", completedWriteBody=" + completedWriteBody + ", completedWriteLastBody=" +
			completedWriteLastBody + ", readResponseHttpHeaders=" + readResponseHttpHeaders + ", responseBodyStart=" + responseBodyStart + ", " +
			"responseBodyCompleted=" + responseBodyCompleted + '}';
	}
}
