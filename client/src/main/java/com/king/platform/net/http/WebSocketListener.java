package com.king.platform.net.http;

public interface WebSocketListener {
	void onConnect(WebSocketConnection connection);

	void onError(Throwable t);

	void onDisconnect();

	void onCloseFrame(int code, String reason);

	void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv);

	void onTextFrame(String payload, boolean finalFragment, int rsv);

}
