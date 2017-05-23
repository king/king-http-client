package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.EventCallback;
import com.king.platform.net.http.SseClient;
import com.king.platform.net.http.SseClientCallback;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class DelegatingAsyncSseClientCallback implements SseClientCallback {
	private final CopyOnWriteArrayList<SseClientCallback> sseClientCallbacks = new CopyOnWriteArrayList<>();
	private final List<EventCallback> eventCallbacks = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<SseClient.DisconnectCallback> disconnectCallbacks = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<SseClient.ConnectCallback> connectCallbacks = new CopyOnWriteArrayList<>();

	private final ConcurrentHashMap<String, List<EventCallback>> eventCallbackMap = new ConcurrentHashMap<>();

	private final Executor httpClientCallbackExecutor;


	DelegatingAsyncSseClientCallback(Executor httpClientCallbackExecutor) {
		this.httpClientCallbackExecutor = httpClientCallbackExecutor;
	}

	@Override
	public void onConnect() {
		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (SseClientCallback callback : sseClientCallbacks) {
					callback.onConnect();
				}

				for (SseClient.ConnectCallback callback : connectCallbacks) {
					callback.onConnect();
				}
			}
		});

	}

	@Override
	public void onDisconnect() {
		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (SseClientCallback callback : sseClientCallbacks) {
					callback.onDisconnect();
				}

				for (SseClient.DisconnectCallback callback : disconnectCallbacks) {
					callback.onDisconnect();
				}
			}
		});

	}

	@Override
	public void onError(final Throwable throwable) {
		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (SseClientCallback callback : sseClientCallbacks) {
					callback.onError(throwable);
				}
			}
		});

	}

	@Override
	public void onEvent(final String lastSentId, final String event, final String data) {
		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (SseClientCallback callback : sseClientCallbacks) {
					callback.onEvent(lastSentId, event, data);
				}

				invokeCallbacks(lastSentId, event, data, eventCallbacks);

				if (event != null) {
					List<EventCallback> eventCallbacks = eventCallbackMap.get(event);
					invokeCallbacks(lastSentId, event, data, eventCallbacks);
				}
			}
		});
	}

	private void invokeCallbacks(String lastSentId, String event, String data, List<EventCallback> eventCallbacks) {
		if (eventCallbacks != null) {
			for (EventCallback eventCallback : eventCallbacks) {
				eventCallback.onEvent(lastSentId, event, data);
			}
		}
	}

	void addSseClientCallbacks(SseClientCallback callback) {
		sseClientCallbacks.add(callback);
	}

	void addEventCallback(String eventName, EventCallback callback) {
		List<EventCallback> eventCallbacks = eventCallbackMap.get(eventName);
		if (eventCallbacks == null) {
			eventCallbacks = new CopyOnWriteArrayList<>();
			List<EventCallback> prevValue = eventCallbackMap.putIfAbsent(eventName, eventCallbacks);
			if (prevValue != null) {
				eventCallbacks = prevValue;
			}
		}
		eventCallbacks.add(callback);
	}

	void addEventCallback(EventCallback callback) {
		eventCallbacks.add(callback);
	}

	void addCloseCallback(SseClient.DisconnectCallback disconnectCallback) {
		disconnectCallbacks.add(disconnectCallback);
	}

	void addConnectCallback(SseClient.ConnectCallback connectCallback) {
		connectCallbacks.add(connectCallback);
	}
}
