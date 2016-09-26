package com.king.platform.net.http;


public interface SseCallback {
	void onEvent(ServerSideEvent serverSideEvent);
}
