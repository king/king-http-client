package com.king.platform.net.http.netty.eventbus;



public class ExternalEventTrigger {

	private EventListener eventListener;

	public <T> void trigger(Event1<T> event, T payload) {
		if (eventListener != null) {
			eventListener.onEvent(event, payload);
		}
	}

	public <T1, T2> void trigger(Event2<T1, T2> event, T1 payload1, T2 payload2) {
		if (eventListener != null) {
			eventListener.onEvent(event, payload1, payload2);
		}
	}

	public void registerEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}
}
