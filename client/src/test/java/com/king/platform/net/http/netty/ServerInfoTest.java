// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerInfoTest {

	@Test
	public void buildFromHttpUriWithDefaultPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://someserver/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(80, serverInfo.getPort());
		assertFalse(serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpUriWithDefaultPortAndUnderscoreHostname() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://some_server/foo/bar");
		assertEquals("some_server", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(80, serverInfo.getPort());
		assertFalse(serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpsUriWithDefaultPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("https://someserver/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("https", serverInfo.getScheme());
		assertEquals(443, serverInfo.getPort());
		assertTrue(serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpsUriWithDefaultPortAndUnderscoreHostname() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("https://some_server/foo/bar");
		assertEquals("some_server", serverInfo.getHost());
		assertEquals("https", serverInfo.getScheme());
		assertEquals(443, serverInfo.getPort());
		assertTrue(serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpUriWithSpecificPort() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://someserver:8081/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
		assertFalse(serverInfo.isSecure());
	}

	@Test
	public void buildFromHttpUriWithSpecificPortAndUnderscoreHostname() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("http://some_server:8081/foo/bar");
		assertEquals("some_server", serverInfo.getHost());
		assertEquals("http", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
		assertFalse(serverInfo.isSecure());
	}

	@Test
	public void buildFromWebService() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("ws://someserver:8081/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("ws", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
	}

	@Test
	public void buildFromWebServiceAndUnderscoreHostname() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("ws://some_server:8081/foo/bar");
		assertEquals("some_server", serverInfo.getHost());
		assertEquals("ws", serverInfo.getScheme());
		assertEquals(8081, serverInfo.getPort());
	}

	@Test
	public void buildFromSecureWebService() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("wss://someserver:8443/foo/bar");
		assertEquals("someserver", serverInfo.getHost());
		assertEquals("wss", serverInfo.getScheme());
		assertEquals(8443, serverInfo.getPort());
		assertTrue(serverInfo.isSecure());
	}

	@Test
	public void buildFromSecureWebServiceAndUnderscoreHostname() throws Exception {
		ServerInfo serverInfo = ServerInfo.buildFromUri("wss://some_server:8443/foo/bar");
		assertEquals("some_server", serverInfo.getHost());
		assertEquals("wss", serverInfo.getScheme());
		assertEquals(8443, serverInfo.getPort());
		assertTrue(serverInfo.isSecure());
	}
}
