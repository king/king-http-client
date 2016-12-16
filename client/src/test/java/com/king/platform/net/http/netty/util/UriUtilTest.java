// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;

import com.king.platform.net.http.netty.requestbuilder.Param;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class UriUtilTest {
	@Test
	public void noPath() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com/");
		assertEquals("/", relativeUri);
	}

	@Test
	public void simplePath() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com/hello");
		assertEquals("/hello", relativeUri);
	}

	@Test
	public void simplePath2() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com/hello/world");
		assertEquals("/hello/world", relativeUri);
	}

	@Test
	public void simplePathWithArguments() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com/hello?world");
		assertEquals("/hello?world", relativeUri);
	}

	@Test
	public void noPathWithArgument() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com/?world");
		assertEquals("/?world", relativeUri);
	}


	@Test
	public void noPathWithArgument2() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com?world");
		assertEquals("/?world", relativeUri);
	}

	@Test
	public void noPathWithArgument3() throws Exception {
		String relativeUri = UriUtil.getRelativeUri("http://www.king.com?hello/world");
		assertEquals("/?hello/world", relativeUri);
	}

	@Test
	public void noParameters() throws Exception {
		String uri = UriUtil.getUriWithParameters("http://www.king.com", Collections.<Param>emptyList());
		assertEquals("http://www.king.com", uri);
	}

	@Test
	public void oneParameters() throws Exception {
		String uri = UriUtil.getUriWithParameters("http://www.king.com", Collections.singletonList(new Param("key", "value")));
		assertEquals("http://www.king.com?key=value", uri);
	}

	@Test
	public void twoParameters() throws Exception {
		List<Param> parameters = new ArrayList<>();
		parameters.add(new Param("key", "value"));
		parameters.add(new Param("key2", "value2"));
		String uri = UriUtil.getUriWithParameters("http://www.king.com", parameters);
		assertEquals("http://www.king.com?key=value&key2=value2", uri);
	}

	@Test
	public void twoParameters2() throws Exception {
		List<Param> parameters = new ArrayList<>();
		parameters.add(new Param("key", "value"));
		parameters.add(new Param("key2", "a b"));
		String uri = UriUtil.getUriWithParameters("http://www.king.com", parameters);
		assertEquals("http://www.king.com?key=value&key2=a%20b", uri);
	}
}
