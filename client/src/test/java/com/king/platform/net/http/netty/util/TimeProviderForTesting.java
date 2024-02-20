// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;


public class TimeProviderForTesting implements TimeProvider {
	private long now;

	public void setNow(long now) {
		this.now = now;
	}

	public void forwardMillis(long millis) {
		this.now += millis;
	}

	public void forwardSeconds(float seconds) {
		forwardMillis((long) (seconds * 1000L));
	}

	@Override
	public long currentTimeInMillis() {
		return now;
	}
}
