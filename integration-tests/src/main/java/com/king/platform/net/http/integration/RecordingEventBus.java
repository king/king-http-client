// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;

import com.king.platform.net.http.netty.eventbus.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public class RecordingEventBus implements RequestEventBus, RootEventBus {
	private final Logger logger = getLogger(getClass());
	private static final AtomicInteger idCounter = new AtomicInteger();

	private final RequestEventBus realRequestEventBus;
	private final CopyOnWriteArrayList<Interaction> interactions;
	private final int id;

	private RecordingEventBus childEventBus;

	private static ConcurrentHashMap<Event, CountDownLatch> lockMap = new ConcurrentHashMap<>();

	public RecordingEventBus(RequestEventBus realRequestEventBus) {
		this.realRequestEventBus = realRequestEventBus;
		interactions = new CopyOnWriteArrayList<>();
		id = idCounter.incrementAndGet();
	}

	@Override
	public <T> void subscribe(Event1<T> event, EventBusCallback1<T> callback) {
		recordInteraction(event, InteractionType.SUBSCRIPTION);
		realRequestEventBus.subscribe(event, callback);
	}

	@Override
	public <T1, T2> void subscribe(Event2<T1, T2> event, EventBusCallback2<T1, T2> callback) {
		recordInteraction(event, InteractionType.SUBSCRIPTION);
		realRequestEventBus.subscribe(event, callback);
	}

	@Override
	public <T> void subscribePermanently(Event1<T> event, EventBusCallback1<T> callback) {
		recordInteraction(event, InteractionType.SUBSCRIPTION);
		realRequestEventBus.subscribePermanently(event, callback);
	}

	@Override
	public <T1, T2> void subscribePermanently(Event2<T1, T2> event, EventBusCallback2<T1, T2> callback) {
		recordInteraction(event, InteractionType.SUBSCRIPTION);
		realRequestEventBus.subscribePermanently(event, callback);
	}

	@Override
	public <T> void triggerEvent(Event1<T> event, T payload) {
		recordInteraction(event, InteractionType.TRIGGER);
		realRequestEventBus.triggerEvent(event, payload);
		CountDownLatch countDownLatch = lockMap.get(event);
		if (countDownLatch != null) {
			countDownLatch.countDown();
		}
	}


	@Override
	public void triggerEvent(Event1<Void> event) {
		recordInteraction(event, InteractionType.TRIGGER);

		realRequestEventBus.triggerEvent(event);

		CountDownLatch countDownLatch = lockMap.get(event);
		if (countDownLatch != null) {
			countDownLatch.countDown();
		}
	}

	@Override
	public <T1, T2> void triggerEvent(Event2<T1, T2> event, T1 payload1, T2 payload2) {
		recordInteraction(event, InteractionType.TRIGGER);

		realRequestEventBus.triggerEvent(event, payload1, payload2);

		CountDownLatch countDownLatch = lockMap.get(event);
		if (countDownLatch != null) {
			countDownLatch.countDown();
		}
	}

	private void recordInteraction(Event event, InteractionType interactionType) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stackTrace[3];


		Interaction interaction = new Interaction(event, caller, interactionType);


		interactions.add(interaction);

		logger.trace(interaction.toString());
	}


	public void printDeepInteractionStack() {
		forEachCreatedEventBus(new VisitedEventBusCallback() {
			@Override
			public void onEventBus(RecordingEventBus recordingEventBus) {
				List<RecordingEventBus.Interaction> triggeredEvents = recordingEventBus.getInteractions();
				for (RecordingEventBus.Interaction triggeredEvent : triggeredEvents) {
					System.out.println("BusId " + recordingEventBus.id + ": " + triggeredEvent);
				}
			}
		});
	}

	@Override
	public RequestEventBus createRequestEventBus() {
		RecordingEventBus childRecordingEventBus = new RecordingEventBus(realRequestEventBus.createRequestEventBus());
		this.childEventBus = childRecordingEventBus;
		return childRecordingEventBus;
	}


	public RecordingEventBus getChildEventBus() {
		return childEventBus;
	}

	public List<Interaction> getInteractions() {
		return interactions;
	}

	public List<RecordingEventBus.Interaction> getFilteredInteractions(final RecordingEventBus.InteractionType type, Event... ignoredEvents) {
		final Set<Event> ignoredEventsSet = new HashSet<>();
		if (ignoredEvents != null) {
			Collections.addAll(ignoredEventsSet, ignoredEvents);
		}

		final List<RecordingEventBus.Interaction> filteredInteractions = new ArrayList<>();

		forEachCreatedEventBus(new VisitedEventBusCallback() {
			@Override
			public void onEventBus(RecordingEventBus recordingEventBus) {
				for (RecordingEventBus.Interaction interaction : recordingEventBus.getInteractions()) {
					if (interaction.getInteractionType() != type) {
						continue;
					}

					if (ignoredEventsSet.contains(interaction.getEvent())) {
						continue;
					}

					filteredInteractions.add(interaction);
				}
			}
		});


		return filteredInteractions;

	}

	public boolean hasTriggered(Event event) {
		for (RecordingEventBus.Interaction interaction : getFilteredInteractions(RecordingEventBus.InteractionType.TRIGGER)) {
			if (event == interaction.getEvent()) {
				return true;
			}
		}
		return false;
	}

	public int getTriggeredCount(Event event) {
		int count = 0;
		for (RecordingEventBus.Interaction interaction : getFilteredInteractions(RecordingEventBus.InteractionType.TRIGGER)) {
			if (event == interaction.getEvent()) {
				count ++;
			}
		}
		return count;
	}


	public void waitFor(Event event) throws InterruptedException {
		if (hasTriggered(event)) {
			return;
		}
		lockMap.computeIfAbsent(event, event1 -> new CountDownLatch(1)).await();
	}


	public void waitFor(Event event, int count) throws InterruptedException {
		lockMap.computeIfAbsent(event, event1 -> new CountDownLatch(1)).await();
	}


	private void forEachCreatedEventBus(VisitedEventBusCallback visitedEventBusCallback) {
		visitedEventBusCallback.onEventBus(this);
		if (childEventBus != null) {
			childEventBus.forEachCreatedEventBus(visitedEventBusCallback);
		}
	}

	public interface VisitedEventBusCallback {
		void onEventBus(RecordingEventBus recordingEventBus);
	}


	public static class Interaction {
		private final Event event;
		private final StackTraceElement caller;
		private final InteractionType interactionType;

		public Interaction(Event event, StackTraceElement caller, InteractionType interactionType) {
			this.event = event;
			this.caller = caller;
			this.interactionType = interactionType;
		}

		public Event getEvent() {
			return event;
		}

		public StackTraceElement getCaller() {
			return caller;
		}

		public InteractionType getInteractionType() {
			return interactionType;
		}

		@Override
		public String toString() {
			switch (interactionType) {

				case SUBSCRIPTION:
					return event.toString() + " subscribed from " + caller.toString();

				case TRIGGER:
					return event.toString() + " triggered by " + caller.toString();


			}

			return "Interaction{" +
				"event=" + event +
				", caller=" + caller +
				", interactionType=" + interactionType +
				'}';
		}
	}

	enum InteractionType {
		SUBSCRIPTION,
		TRIGGER
	}
}
