// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;


public interface EventBusCallback2<T1, T2> extends EventBusCallback {
	void onEvent(Event2<T1, T2> event, T1 payload1, T2 payload2);
}
