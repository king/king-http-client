// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;


public class StringUtil {

	public static String substringBefore(String value, char delim) {
		if (value == null) {
			return null;
		}

		int pos = value.indexOf(delim);
		if (pos >= 0) {
			return value.substring(0, pos);
		}
		return value;
	}

	public static String substringAfter(String value, char delim) {
		if (value == null) {
			return null;
		}

		int pos = value.indexOf(delim);
		if (pos >= 0) {
			return value.substring(pos + 1);
		}
		return null;
	}
}
