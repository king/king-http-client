// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;

/**
 * Recorded relative times (in ts ms),
 * Only fully populated if the request was succeeded.
 */
public interface RecordedTimeStamps {
	/**
	 * @return the ts for when the request was created
	 */
	long getCreatedRequest();

	/**
	 * @return the ts for when the client started to write headers to the server
	 */
	long getStartWriteHeaders();

	/**
	 * @return the ts for when the client completed writing headers to the server
	 */
	long getCompletedWriteHeaders();

	/**
	 * @return the ts for when the client starts writing the body to the server (for PUT/POST)
	 */
	long getStartWriteBody();

	/**
	 * @return the ts for when the client completes the writing of the body to the server (for PUT/POST)
	 */
	long getCompletedWriteBody();

	/**
	 * @return the ts for when the client wrote the last http content to the server
	 */
	long getCompletedWriteLastBody();

	/**
	 * @return the ts for when the client read the http headers from the server response
	 */
	long getReadResponseHttpHeaders();

	/**
	 * @return the ts for when the client started to read the response body from the server
	 */
	long getResponseBodyStart();

	/**
	 * @return ths ts for when the client read the complete response body from the server
	 */
	long getResponseBodyCompleted();

	/**
	 * @return the calculated time for the full request/response against the server
	 */
	long getCompleteRequestTime();

	/**
	 * @return the calculated time for the request part
	 */
	long getRequestTime();

	/**
	 * @return the calculated time for the response part
	 */
	long getResponseTime();

	/**
	 * @return the calculated time for the time it took the server (including network delays) to respond to the request
	 */
	long getServerProcessTime();
}
