package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.eventbus.RequestEventBus;

public interface CustomCallbackSubscriber {
	void subscribeOn(RequestEventBus requestEventBus);
}
