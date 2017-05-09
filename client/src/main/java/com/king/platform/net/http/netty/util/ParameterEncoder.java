// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;

public class ParameterEncoder {

	public void addParameter(StringBuilder target, String name, String value) {
		target.append(encodeComponent(name));
		if (value != null) {
			target.append("=");
			target.append(encodeComponent(value));
		}
		target.append("&");
	}


	private String encodeComponent(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedCharsetException("UTF-8");
		}

	}


}
