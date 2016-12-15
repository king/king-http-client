// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.backpressure;

import com.king.platform.net.http.netty.ServerInfo;
import org.junit.Test;

import static org.junit.Assert.*;


public class EvictingBackPressureTest {
	@Test
	public void shouldNotAllowMoreThenAllowed() throws Exception {
		EvictingBackPressure evictingBackPressure = new EvictingBackPressure(2);
		ServerInfo serverInfo = new ServerInfo("http", "localhost", 8008);

		assertTrue(evictingBackPressure.acquireSlot(serverInfo));
		assertTrue(evictingBackPressure.acquireSlot(serverInfo));

		assertFalse(evictingBackPressure.acquireSlot(serverInfo));
		evictingBackPressure.releaseSlot(serverInfo);


		evictingBackPressure.releaseSlot(serverInfo);
		assertTrue(evictingBackPressure.acquireSlot(serverInfo));

		evictingBackPressure.releaseSlot(serverInfo);
		evictingBackPressure.releaseSlot(serverInfo);

		assertTrue(evictingBackPressure.acquireSlot(serverInfo));
		assertTrue(evictingBackPressure.acquireSlot(serverInfo));

	}
}
