// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;


import com.king.platform.net.http.netty.ServerInfo;
import io.netty.channel.Channel;

public class NoChannelPool implements ChannelPool {

	@Override
	public Channel get(ServerInfo serverInfo) {
		return null;
	}

	@Override
	public void offer(ServerInfo serverInfo, Channel channel, int keepAliveTimeoutMillis) {
		channel.close();
	}

	@Override
	public void discard(ServerInfo serverInfo, Channel channel) {
		channel.close();
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public void shutdown() {

	}
}
