// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class ServerInfoTest {

	@Test
	public void buildFromHttpUriWithDefaultPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://someserver/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(80, serverInfo.getPort());
		assertEquals(false, serverInfo.isSecure());

	}

	@Test
	public void buildFromHttpsUriWithDefaultPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("https://someserver/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("https", serverInfo.getScheme());
		assertEquals(443, serverInfo.getPort());
		assertEquals(true, serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpUriWithSpecificPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://someserver:8081/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
		assertEquals(false, serverInfo.isSecure());

	}

	@Test
	public void buildFromWebService() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("ws://someserver:8081/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("ws", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
	}

	@Test
	public void buildFromSecureWebService() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("wss://someserver:8443/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("wss", serverInfo.getScheme());
		assertEquals(8443, serverInfo.getPort());
		assertEquals(true, serverInfo.isSecure());

	}


}
