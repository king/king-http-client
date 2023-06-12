// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringUtilTest {
	@BeforeEach
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


	@ParameterizedTest
	@CsvSource(value = {
		"text/plain; charset=utf-8:utf-8",
		"text/plain;charset=ISO-8851-1:ISO-8851-1",
		"text/plain;charset=ISO-8851-1:ISO-8851-1",
		"text/plain;charset=\"ISO-8851-1\":ISO-8851-1",
		"text/plain; version=0.0.4; charset=utf-8:utf-8",
		"text/plain; charset=utf-8; version=0.0.4:utf-8",
		"text/plain; version   = 0.0.4   ; charset =  utf-8  :utf-8",
		"text/plain; charset   = utf-8   ; version =  0.0.4  :utf-8"
	}, delimiter = ':')
	void substringKeyValueTest(String input, String expected) {
		assertEquals(expected, StringUtil.substringKeyValue("charset", input, ';', true));
	}
	@Test
	void substringKeyValueWithMissingValueOrKeyTest() {
		assertNull(StringUtil.substringKeyValue(null, null, ';', true));
		assertNull(StringUtil.substringKeyValue(null, "aaa", ';', true));
		assertNull(StringUtil.substringKeyValue("aaa", null, ';', true));
		assertNull(StringUtil.substringKeyValue("ddd", "aaa=1;bbb=;ccc", ';', true));

		assertEquals("", StringUtil.substringKeyValue("bbb", "aaa=1;bbb=;ccc", ';', true));
		assertEquals("", StringUtil.substringKeyValue("ccc", "aaa=1;bbb=;ccc", ';', true));
	}

}
