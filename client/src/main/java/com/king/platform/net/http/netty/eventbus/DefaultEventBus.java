// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

public class DefaultEventBus implements RequestEventBus, RootEventBus {
	private final Logger logger = getLogger(getClass());

	private final AtomicBoolean hasTriggeredCompleted = new AtomicBoolean();
	private final AtomicBoolean hasTriggeredError = new AtomicBoolean();

	private final ConcurrentMap<Event1, List<EventBusCallback1>> event1Callbacks;
	private final ConcurrentMap<Event2, List<EventBusCallback2>> event2Callbacks;

	private final ConcurrentMap<Event1, List<EventBusCallback1>> persistentEvent1Callbacks;
	private final ConcurrentMap<Event2, List<EventBusCallback2>> persistentEvent2Callbacks;


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


	private <E extends Event, B extends EventBusCallback> void subscribe(ConcurrentMap<E, List<B>> map, E event, B callback) {
		if (!map.containsKey(event)) {
			map.putIfAbsent(event, new ArrayList<>());
		}

		map.get(event).add(callback);
	}


	@Override
	public void triggerEvent(Event1<Void> event) {
		triggerEvent(event, null);
	}

	@Override
	public <T> void triggerEvent(Event1<T> event, T payload) {

		if (validEvent(event)) {
			triggerEvent1(event, payload, persistentEvent1Callbacks);
			triggerEvent1(event, payload, event1Callbacks);
		} else {
			logger.error("Invalid event order, tried to trigger event {} with payload {}, but hasTriggeredCompleted was {} and hasTriggeredError was {}",
				event, payload, hasTriggeredCompleted.get(), hasTriggeredError.get());

		}
	}


	@Override
	public <T1, T2> void triggerEvent(Event2<T1, T2> event, T1 payload1, T2 payload2) {
		if (validEvent(event)) {
			triggerEvent2(event, payload1, payload2, persistentEvent2Callbacks);
			triggerEvent2(event, payload1, payload2, event2Callbacks);
		} else {
			logger.error("Invalid event order, tried to trigger event {} with payload1 {}, payload2 {} but hasTriggeredCompleted was {} and hasTriggeredError was {}",
				event, payload1, payload2, hasTriggeredCompleted.get(), hasTriggeredError.get());

			if (event == Event.ERROR && payload2 instanceof Throwable) {
				Throwable throwable = (Throwable) payload2;
				logger.trace("Triggered error had throwable", throwable);
			}
		}
	}


	private <T> void triggerEvent1(Event1<T> event, T payload, ConcurrentMap<Event1, List<EventBusCallback1>> callbacks) {
		if (!callbacks.containsKey(event)) {
			return;
		}

		for (EventBusCallback1 eventBusCallback : callbacks.get(event)) {
			EventBusCallback1<T> callback = (EventBusCallback1<T>) eventBusCallback;
			callback.onEvent(event, payload);
		}
	}

	private <T1, T2> void triggerEvent2(Event2<T1, T2> event, T1 payload1, T2 payload2, ConcurrentMap<Event2, List<EventBusCallback2>> callbacks) {
		if (!callbacks.containsKey(event)) {
			return;
		}

		for (EventBusCallback eventBusCallback : callbacks.get(event)) {
			EventBusCallback2<T1, T2> callback = (EventBusCallback2<T1, T2>) eventBusCallback;
			callback.onEvent(event, payload1, payload2);
		}
	}


	private boolean validEvent(Event event) {
		if (event == Event.COMPLETED) {
			if (hasTriggeredError.get()) {
				return false;
			}
			hasTriggeredCompleted.set(true);
		}

		if (event == Event.ERROR) {
			hasTriggeredError.set(true);
		}

		return true;
	}


	@Override
	public RequestEventBus createRequestEventBus() {
		DefaultEventBus cleanEventBus = new DefaultEventBus();

		for (Map.Entry<Event1, List<EventBusCallback1>> entry : persistentEvent1Callbacks.entrySet()) {
			for (EventBusCallback1 eventBusCallback : entry.getValue()) {
				cleanEventBus.subscribePermanently(entry.getKey(), eventBusCallback);
			}
		}

		for (Map.Entry<Event2, List<EventBusCallback2>> entry : persistentEvent2Callbacks.entrySet()) {
			for (EventBusCallback2 eventBusCallback : entry.getValue()) {
				cleanEventBus.subscribePermanently(entry.getKey(), eventBusCallback);
			}
		}

		return cleanEventBus;
	}
}
