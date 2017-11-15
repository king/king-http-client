// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.backpressure;

import com.king.platform.net.http.netty.ServerInfo;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class BlockingBackPressureTest {

	@Test
	public void shouldBlockIfTooManyConnections() throws Exception {
		final BlockingBackPressure blockingBackPressure = new BlockingBackPressure(1);
		final ServerInfo serverInfo = new ServerInfo("http", "localhost", 8008, false, false);

		assertTrue(blockingBackPressure.acquireSlot(serverInfo));

		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);

		final AtomicBoolean acquired = new AtomicBoolean();

		new Thread(new Runnable() {
			@Override
			public void run() {
				latch1.countDown();
				acquired.set(blockingBackPressure.acquireSlot(serverInfo));
				latch2.countDown();
			}
		}).start();

		latch1.await();
		Thread.sleep(10);
		assertFalse(acquired.get());
		blockingBackPressure.releaseSlot(serverInfo);
		latch2.await();
		assertTrue(acquired.get());

	}
}
