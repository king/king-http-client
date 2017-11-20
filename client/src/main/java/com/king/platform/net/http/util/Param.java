// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.util;


public class Param {
	private final CharSequence name;
	private final CharSequence value;

	public Param(CharSequence name, CharSequence value) {
		this.name = name;
		this.value = value;
	}

	public CharSequence getName() {
		return name;
	}

	public CharSequence getValue() {
		return value;
	}
}
