// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;

import com.king.platform.net.http.netty.request.ChunkedFileHttpBody;
import com.king.platform.net.http.netty.request.FileRegionHttpBody;
import com.king.platform.net.http.netty.request.HttpBody;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.*;


public class FileHttpBodyBuilderTest {
	private FileHttpBodyBuilder fileHttpBodyBuilder;
	private String contentType;

	@Before
	public void setUp() throws Exception {
		fileHttpBodyBuilder = new FileHttpBodyBuilder(new File("./tmp"));
		contentType = "contentType";
	}

	@Test
	public void insecureShouldReturnFileRegion() throws Exception {
		HttpBody httpBody = fileHttpBodyBuilder.createHttpBody(contentType, Charset.defaultCharset(), false);
		assertEquals(FileRegionHttpBody.class, httpBody.getClass());
		assertEquals(contentType, httpBody.getContentType());
	}

	@Test
	public void secureShouldReturnChunkedFile() throws Exception {
		HttpBody httpBody = fileHttpBodyBuilder.createHttpBody(contentType, Charset.defaultCharset(), true);
		assertEquals(ChunkedFileHttpBody.class, httpBody.getClass());
		assertEquals(contentType, httpBody.getContentType());
	}
}
