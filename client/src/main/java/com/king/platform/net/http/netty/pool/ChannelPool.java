// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;


import com.king.platform.net.http.netty.ServerInfo;
import io.netty.channel.Channel;

public interface ChannelPool {
	Channel get(ServerInfo serverInfo);

	void offer(ServerInfo serverInfo, Channel channel);

	void discard(ServerInfo serverInfo, Channel channel);

	boolean isActive();

	void shutdown();
}
