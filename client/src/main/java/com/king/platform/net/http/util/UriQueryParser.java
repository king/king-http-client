// Copyright (C) king.com Ltd 2017
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.util;


import io.netty.handler.codec.http.QueryStringDecoder;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class UriQueryParser {
	private final QueryStringDecoder queryStringDecoder;
	private final Map<String, List<String>> parameters;

	public UriQueryParser(String url) {
		queryStringDecoder = new QueryStringDecoder(url);
		parameters = queryStringDecoder.parameters();
	}

	public UriQueryParser(String url, Charset charset) {
		queryStringDecoder = new QueryStringDecoder(url, charset);
		parameters = queryStringDecoder.parameters();
	}

	public List<String> getValue(String key) {
		return parameters.get(key);
	}

	public String getValueOrDefault(String key, String defValue) {
		List<String> strings = parameters.get(key);
		if (strings == null || strings.isEmpty()) {
			return defValue;
		}
		return strings.get(0);
	}

	public boolean hasKey(String key) {
		return parameters.containsKey(key);
	}

}
