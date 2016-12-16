// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;

import com.king.platform.net.http.netty.util.TimeProviderForTesting;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TimeStampRecorderTest {
	private TimeStampRecorder timeStampRecorder;
	private TimeProviderForTesting timeProvider;

	@Before
	public void setUp() throws Exception {
		timeProvider = new TimeProviderForTesting();
		timeStampRecorder = new TimeStampRecorder(timeProvider);
		timeProvider.setNow(5005);
	}

	@Test
	public void recordCreatedRequest() throws Exception {
		timeStampRecorder.recordCreatedRequest();
		assertEquals(5005, timeStampRecorder.getCreatedRequest());
	}

	@Test
	public void startWriteHeaders() throws Exception {
		timeStampRecorder.startWriteHeaders();
		assertEquals(5005, timeStampRecorder.getStartWriteHeaders());
	}

	@Test
	public void completedWriteHeaders() throws Exception {
		timeStampRecorder.completedWriteHeaders();
		assertEquals(5005, timeStampRecorder.getCompletedWriteHeaders());
	}

	@Test
	public void startWriteBody() throws Exception {
		timeStampRecorder.startWriteBody();
		assertEquals(5005, timeStampRecorder.getStartWriteBody());
	}

	@Test
	public void completedWriteBody() throws Exception {
		timeStampRecorder.completedWriteBody();
		assertEquals(5005, timeStampRecorder.getCompletedWriteBody());
	}

	@Test
	public void completedWriteLastBody() throws Exception {
		timeStampRecorder.completedWriteLastBody();
		assertEquals(5005, timeStampRecorder.getCompletedWriteLastBody());
	}

	@Test
	public void readResponseHttpHeaders() throws Exception {
		timeStampRecorder.readResponseHttpHeaders();
		assertEquals(5005, timeStampRecorder.getReadResponseHttpHeaders());
	}

	@Test
	public void responseBodyStart() throws Exception {
		timeStampRecorder.responseBodyStart();
		assertEquals(5005, timeStampRecorder.getResponseBodyStart());
	}

	@Test
	public void responseBodyCompleted() throws Exception {
		timeStampRecorder.responseBodyCompleted();
		assertEquals(5005, timeStampRecorder.getResponseBodyCompleted());
	}

	@Test
	public void getCompleteRequestTime() throws Exception {
		timeProvider.setNow(1000);
		timeStampRecorder.recordCreatedRequest();
		timeProvider.setNow(2000);
		timeStampRecorder.responseBodyCompleted();
		assertEquals(1000, timeStampRecorder.getCompleteRequestTime());
	}


	@Test
	public void getRequestTime() throws Exception {
		timeProvider.setNow(1000);
		timeStampRecorder.startWriteHeaders();
		timeProvider.setNow(2000);
		timeStampRecorder.completedWriteLastBody();
		assertEquals(1000, timeStampRecorder.getRequestTime());
	}


	@Test
	public void getResponseTime() throws Exception {
		timeProvider.setNow(1000);
		timeStampRecorder.readResponseHttpHeaders();
		timeProvider.setNow(2000);
		timeStampRecorder.responseBodyCompleted();
		assertEquals(1000, timeStampRecorder.getResponseTime());
	}


	@Test
	public void getServerProcessTime() throws Exception {
		timeProvider.setNow(1000);
		timeStampRecorder.completedWriteLastBody();
		timeProvider.setNow(2000);
		timeStampRecorder.readResponseHttpHeaders();
		assertEquals(1000, timeStampRecorder.getServerProcessTime());
	}


}
