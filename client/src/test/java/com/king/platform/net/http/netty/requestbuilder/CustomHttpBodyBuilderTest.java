// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;

import com.king.platform.net.http.netty.request.HttpBody;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertSame;
import static se.mockachino.Mockachino.mock;


public class CustomHttpBodyBuilderTest {
	@Test
	public void createHttpBodyShouldReturnProvidedBody() throws Exception {
		HttpBody httpBody = mock(HttpBody.class);
		CustomHttpBodyBuilder customHttpBodyBuilder = new CustomHttpBodyBuilder(httpBody);
		assertSame(httpBody, customHttpBodyBuilder.createHttpBody("contentType", Charset.defaultCharset(), false));

	}
}
