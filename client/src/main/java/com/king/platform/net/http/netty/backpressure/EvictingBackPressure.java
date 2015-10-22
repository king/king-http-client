// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.backpressure;

import com.king.platform.net.http.netty.ServerInfo;

import java.util.concurrent.atomic.AtomicInteger;

public class EvictingBackPressure implements BackPressure {
	private final int maxTotalParallelConnections;

	private final AtomicInteger currentParallelConnections = new AtomicInteger();

	public EvictingBackPressure(int maxTotalParallelConnections) {
		this.maxTotalParallelConnections = maxTotalParallelConnections;
	}

	@Override
	public boolean acquireSlot(ServerInfo serverInfo) {
		int current = currentParallelConnections.incrementAndGet();
		return current <= maxTotalParallelConnections;
	}

	@Override
	public void releaseSlot(ServerInfo serverInfo) {
		currentParallelConnections.decrementAndGet();
	}

}
