// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;


public interface RecordedTimeStamps {
	long getCreatedRequest();

	long getStartWriteHeaders();

	long getCompletedWriteHeaders();

	long getStartWriteBody();

	long getCompletedWriteBody();

	long getCompletedWriteLastBody();

	long getReadResponseHttpHeaders();

	long getResponseBodyStart();

	long getResponseBodyCompleted();
}
