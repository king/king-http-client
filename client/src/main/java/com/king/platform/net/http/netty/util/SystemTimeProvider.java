// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;


public class SystemTimeProvider implements TimeProvider {

	@Override
	public long currentTimeInMillis() {
		return System.currentTimeMillis();
	}


	@Override
	public long currentTimeInNanos() {
		return System.nanoTime();
	}
}
