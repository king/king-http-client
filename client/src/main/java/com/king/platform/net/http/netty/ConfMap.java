// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.ConfKeys;

import java.util.HashMap;
import java.util.Map;

public class ConfMap {
	private final Map<ConfKeys, Object> confKeysMap = new HashMap<>();

	public ConfMap() {
	}

	public <T> void set(ConfKeys<T> key, T value) {
		confKeysMap.put(key, value);
	}

	public <T> T get(ConfKeys<T> key) {
		if (confKeysMap.containsKey(key)) {
			return (T) confKeysMap.get(key);
		} else {
			return key.getDefaultValue();
		}
	}
}
