// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;

public class NettyChannelOptions {
	private final Map<ChannelOption, Object> options = new HashMap<>();

	public <T> void add(ChannelOption<T> name, T value) {
		options.put(name, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getOption(ChannelOption<T> name) {
		return (T) options.get(name);
	}

	public Iterable<ChannelOption> keys() {
		return options.keySet();
	}

	public Object get(ChannelOption channelOption) {
		return options.get(channelOption);
	}
}
