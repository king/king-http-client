// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringUtilTest {
	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void substringBefore() throws Exception {
		assertEquals("text/html", StringUtil.substringBefore("text/html; charset=utf-8", ';'));

		assertEquals("", StringUtil.substringBefore(";charset=utf-8", ';'));

		assertEquals("text/html", StringUtil.substringBefore("text/html", ';'));
	}

	@Test
	public void substringAfterRemoveQuoteMarks() throws Exception {
		assertEquals("utf-8", StringUtil.substringAfter("text/html; charset=utf-8", '=', true));
		assertEquals("", StringUtil.substringAfter("text/html; charset=", '=', true));

		//we should support quoted charsets as well
		assertEquals("utf-8", StringUtil.substringAfter("text/html; charset=\"utf-8\"", '=', true));
		assertEquals("utf-8", StringUtil.substringAfter("text/html; charset='utf-8'", '=', true));

		assertNull(StringUtil.substringAfter("text/html", '=', true));
	}

	@Test
	public void substringAfterIgnoreQuoteMarks() throws Exception {
		assertEquals("utf-8", StringUtil.substringAfter("text/html; charset=utf-8", '=', false));
		assertEquals("", StringUtil.substringAfter("text/html; charset=", '=', false));

		//we should support quoted charsets as well
		assertEquals("\"utf-8\"", StringUtil.substringAfter("text/html; charset=\"utf-8\"", '=', false));
		assertEquals("'utf-8'", StringUtil.substringAfter("text/html; charset='utf-8'", '=', false));

		assertNull(StringUtil.substringAfter("text/html", '=', false));
	}
}
