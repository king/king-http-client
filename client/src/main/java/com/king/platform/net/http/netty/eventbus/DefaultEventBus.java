// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

public class DefaultEventBus implements RequestEventBus, RootEventBus {
	private final Logger logger = getLogger(getClass());

	private final ConcurrentHashMap<Event, ArrayList<EventBusCallback>> event1Callbacks;
	private final ConcurrentHashMap<Event, ArrayList<EventBusCallback>> event2Callbacks;

	private final ConcurrentHashMap<Event, ArrayList<EventBusCallback>> persistentEvent1Callbacks;
	private final ConcurrentHashMap<Event, ArrayList<EventBusCallback>> persistentEvent2Callbacks;


	public DefaultEventBus() {
		event1Callbacks = new ConcurrentHashMap<>();
		event2Callbacks = new ConcurrentHashMap<>();

		persistentEvent1Callbacks = new ConcurrentHashMap<>();
		persistentEvent2Callbacks = new ConcurrentHashMap<>();
	}


	@Override
	public <T> void subscribe(Event1<T> event, EventBusCallback1<T> callback) {
		subscribe(event1Callbacks, event, callback);
	}


	@Override
	public <T1, T2> void subscribe(Event2<T1, T2> event, EventBusCallback2<T1, T2> callback) {
		subscribe(event2Callbacks, event, callback);
	}


	@Override
	public <T> void subscribePermanently(Event1<T> event, EventBusCallback1<T> callback) {
		subscribe(persistentEvent1Callbacks, event, callback);
	}

	@Override
	public <T1, T2> void subscribePermanently(Event2<T1, T2> event, EventBusCallback2<T1, T2> callback) {
		subscribe(persistentEvent2Callbacks, event, callback);
	}


	private void subscribe(ConcurrentHashMap<Event, ArrayList<EventBusCallback>> map, Event event, EventBusCallback callback) {
		ArrayList<EventBusCallback> eventList = map.get(event);

		if (eventList == null) {
			eventList = new ArrayList<>();
			ArrayList<EventBusCallback> oldValue = map.putIfAbsent(event, eventList);
			if (oldValue != null) {
				eventList = oldValue;
			}
		}
		eventList.add(callback);
	}


	@Override
	public void triggerEvent(Event1<Void> event) {
		triggerEvent(event, null);

	}

	@Override
	public <T> void triggerEvent(Event1<T> event, T payload) {
		triggerEvent1(event, payload, persistentEvent1Callbacks);
		triggerEvent1(event, payload, event1Callbacks);
	}


	@Override
	public <T1, T2> void triggerEvent(Event2<T1, T2> event, T1 payload1, T2 payload2) {
		triggerEvent2(event, payload1, payload2, persistentEvent2Callbacks);
		triggerEvent2(event, payload1, payload2, event2Callbacks);
	}


	private <T> void triggerEvent1(Event1<T> event, T payload, ConcurrentHashMap<Event, ArrayList<EventBusCallback>> callbacks) {
		ArrayList<EventBusCallback> eventBusCallback1s = callbacks.get(event);

		if (eventBusCallback1s == null) {
			return;
		}

		for (EventBusCallback eventBusCallback : eventBusCallback1s) {
			EventBusCallback1<T> callback = (EventBusCallback1<T>) eventBusCallback;
			callback.onEvent(event, payload);
		}
	}

	private <T1, T2> void triggerEvent2(Event2<T1, T2> event, T1 payload1, T2 payload2, ConcurrentHashMap<Event, ArrayList<EventBusCallback>> callbacks) {
		ArrayList<EventBusCallback> eventBusCallback2s = callbacks.get(event);

		if (eventBusCallback2s == null) {
			return;
		}

		for (EventBusCallback eventBusCallback : eventBusCallback2s) {
			EventBusCallback2<T1, T2> callback = (EventBusCallback2<T1, T2>) eventBusCallback;
			callback.onEvent(event, payload1, payload2);
		}
	}


	@Override
	public RequestEventBus createRequestEventBus() {
		DefaultEventBus cleanEventBus = new DefaultEventBus();

		for (Map.Entry<Event, ArrayList<EventBusCallback>> entry : persistentEvent1Callbacks.entrySet()) {
			for (EventBusCallback eventBusCallback : entry.getValue()) {
				cleanEventBus.subscribePermanently((Event1) entry.getKey(), (EventBusCallback1) eventBusCallback);
			}
		}

		for (Map.Entry<Event, ArrayList<EventBusCallback>> entry : persistentEvent2Callbacks.entrySet()) {
			for (EventBusCallback eventBusCallback : entry.getValue()) {
				cleanEventBus.subscribePermanently((Event2) entry.getKey(), (EventBusCallback2) eventBusCallback);
			}
		}

		return cleanEventBus;

	}


}
