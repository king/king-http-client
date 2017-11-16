package com.king.platform.net.http;

public interface WebSocketListener {
	void onConnect(WebSocketConnection connection);

	void onDisconnect(int code, String reason);

	void onError(Throwable t);

	void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv);

	void onTextFrame(String payload, boolean finalFragment, int rsv);

}
