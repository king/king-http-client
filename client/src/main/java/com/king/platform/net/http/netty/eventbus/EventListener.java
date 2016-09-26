package com.king.platform.net.http.netty.eventbus;


public interface EventListener {
	<T> void onEvent(Event1<T> event, T payload);

	<T1, T2> void onEvent(Event2<T1, T2> event, T1 payload1, T2 payload2);
}
