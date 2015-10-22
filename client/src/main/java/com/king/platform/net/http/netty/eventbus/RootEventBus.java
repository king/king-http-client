// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;


public interface RootEventBus {
	<T> void subscribePermanently(Event1<T> event, EventBusCallback1<T> callback);

	<T1, T2> void subscribePermanently(Event2<T1, T2> event, EventBusCallback2<T1, T2> callback);

	RequestEventBus createRequestEventBus();

}
