package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.ServerSideEvent;

class ServerSideEventImpl implements ServerSideEvent {
	private final String lastSentId;
	private final String event;
	private final String data;

	ServerSideEventImpl(String lastSentId, String event, String data) {
		this.lastSentId = lastSentId;
		this.event = event;
		this.data = data;
	}

	@Override
	public String lastId() {
		return lastSentId;
	}

	@Override
	public String data() {
		return data;
	}

	@Override
	public String event() {
		return event;
	}
}
