// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.ResponseBodyConsumer;

import java.nio.ByteBuffer;

class VoidResponseConsumer implements ResponseBodyConsumer<Void> {
	@Override
	public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
	}

	@Override
	public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
	}

	@Override
	public void onCompletedBody() throws Exception {
	}

	@Override
	public Void getBody() {
		return null;
	}
}
