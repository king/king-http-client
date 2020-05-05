// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@SuppressWarnings("unchecked")
public class HttpResponseTest {
	@Test
	public void getHeaderShouldBeCaseInsensitive() throws Exception {
		DefaultHttpHeaders defaultHttpHeaders = new DefaultHttpHeaders();
		defaultHttpHeaders.add("Accept", "*/*");

		HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, defaultHttpHeaders);

		assertEquals("*/*", httpResponse.getHeader("Accept"));
		assertEquals("*/*", httpResponse.getHeader("ACCEPT"));
		assertEquals("*/*", httpResponse.getHeader("accept"));

	}

	@Test
	public void getHeadersShouldBeCaseInsensitive() throws Exception {

		DefaultHttpHeaders defaultHttpHeaders = new DefaultHttpHeaders();
		defaultHttpHeaders.add("Accept", "*/*");
		defaultHttpHeaders.add("ACCEPT", "*/*");
		defaultHttpHeaders.add("accept", "*/*");

		HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, defaultHttpHeaders);
		List<String> headers = httpResponse.getHeaders("accept");
		assertEquals(3, headers.size());
		for (String header : headers) {
			assertEquals("*/*", header);
		}
	}

	@Test
	public void getUnknownHeaderShouldReturnNull() throws Exception {
		HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, new DefaultHttpHeaders());
		String value = httpResponse.getHeader("undefined");
		assertNull(value);
	}

	@Test
	public void getAllHeaders() throws Exception {
		DefaultHttpHeaders defaultHttpHeaders = new DefaultHttpHeaders();
		defaultHttpHeaders.add("Accept", "*/*");
		defaultHttpHeaders.add("ACCEPT", "*/*");
		defaultHttpHeaders.add("accept", "*/*");

		HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, defaultHttpHeaders);
		List<Map.Entry<String, String>> allHeaders = httpResponse.getAllHeaders();
		assertEquals(3, allHeaders.size());
		for (Map.Entry<String, String> entry : allHeaders) {
			assertEquals("*/*", entry.getValue());
		}

	}

	@Test
	public void getStatus() {
		final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, new DefaultHttpHeaders());
		assertEquals(httpResponse.getStatusCode(), HttpResponseStatus.OK.code());
		assertEquals(httpResponse.getStatusReason(), HttpResponseStatus.OK.reasonPhrase());
	}

	@Test
	public void getHttpVersion() {
		final HttpResponse httpResponse = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, null, new DefaultHttpHeaders());
		assertEquals(httpResponse.getHttpVersion(), HttpVersion.HTTP_1_1.toString());
	}
}
