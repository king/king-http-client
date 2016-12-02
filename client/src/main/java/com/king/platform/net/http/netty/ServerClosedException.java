// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import java.io.IOException;

public class ServerClosedException extends IOException {
	public ServerClosedException() {
	}

	public ServerClosedException(String message) {
		super(message);
	}

	public ServerClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerClosedException(Throwable cause) {
		super(cause);
	}
}
