package com.king.platform.net.http.netty.websocket;

import java.io.IOException;

public class WebSocketException extends IOException {
	public WebSocketException() {
	}

	public WebSocketException(String message) {
		super(message);
	}

	public WebSocketException(String message, Throwable cause) {
		super(message, cause);
	}

	public WebSocketException(Throwable cause) {
		super(cause);
	}
}
