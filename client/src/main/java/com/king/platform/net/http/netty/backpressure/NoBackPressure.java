// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.backpressure;

import com.king.platform.net.http.netty.ServerInfo;

public class NoBackPressure implements BackPressure {
	@Override
	public boolean acquireSlot(ServerInfo serverInfo) {
		return true;
	}

	@Override
	public void releaseSlot(ServerInfo serverInfo) {

	}
}
