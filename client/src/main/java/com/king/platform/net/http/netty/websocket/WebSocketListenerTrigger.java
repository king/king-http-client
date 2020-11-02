package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.WebSocketConnection;
import com.king.platform.net.http.WebSocketFrameListener;
import com.king.platform.net.http.WebSocketListener;
import com.king.platform.net.http.WebSocketMessageListener;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class WebSocketListenerTrigger implements WebSocketFrameListener, WebSocketMessageListener, WebSocketListener {
	private final CopyOnWriteArrayList<WebSocketFrameListener> frameListeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<WebSocketMessageListener> messageListeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<WebSocketListener> legacyListeners = new CopyOnWriteArrayList<>();
	private final Executor executor;

	public WebSocketListenerTrigger(Executor executor) {
		this.executor = executor;
	}

	public void addListener(WebSocketFrameListener listener) {
		frameListeners.add(listener);
	}

	public void addListener(WebSocketMessageListener listener) {
		messageListeners.add(listener);
	}

	public void addListener(WebSocketListener listener) {
		legacyListeners.add(listener);
	}

	@Override
	public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onBinaryFrame(payload, finalFragment, rsv);
			}
		});
	}

	@Deprecated
	public void onLegacyBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
		executor.execute(() -> {
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onBinaryFrame(payload, finalFragment, rsv);
			}
		});
	}

	@Override
	public void onTextFrame(byte[] payload, boolean finalFragment, int rsv) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onTextFrame(payload, finalFragment, rsv);
			}
		});
	}

	@Override
	public void onTextFrame(String payload, boolean finalFragment, int rsv) {
		executor.execute(() -> {
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onTextFrame(payload, finalFragment, rsv);
			}
		});
	}

	@Deprecated
	public void onLegacyTextFrame(String payload, boolean finalFragment, int rsv) {
		executor.execute(() -> {
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onTextFrame(payload, finalFragment, rsv);
			}
		});
	}

	@Override
	public void onBinaryMessage(byte[] message) {
		executor.execute(() -> {
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onBinaryMessage(message);
			}
		});
	}

	@Override
	public void onTextMessage(String message) {
		executor.execute(() -> {
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onTextMessage(message);
			}
		});
	}

	@Override
	public void onConnect(WebSocketConnection connection) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onConnect(connection);
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onConnect(connection);
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onConnect(connection);
			}
		});
	}

	@Override
	public void onError(Throwable throwable) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onError(throwable);
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onError(throwable);
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onError(throwable);
			}
		});
	}

	@Override
	public void onDisconnect() {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onDisconnect();
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onDisconnect();
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onDisconnect();
			}
		});
	}

	@Override
	public void onCloseFrame(int code, String reason) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onCloseFrame(code, reason);
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onCloseFrame(code, reason);
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onCloseFrame(code, reason);
			}
		});
	}

	@Override
	public void onPingFrame(byte[] payload) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onPingFrame(payload);
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onPingFrame(payload);
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onPingFrame(payload);
			}
		});
	}

	@Override
	public void onPongFrame(byte[] payload) {
		executor.execute(() -> {
			for (WebSocketFrameListener frameListener : frameListeners) {
				frameListener.onPongFrame(payload);
			}
			for (WebSocketMessageListener messageListener : messageListeners) {
				messageListener.onPongFrame(payload);
			}
			for (WebSocketListener legacyListener : legacyListeners) {
				legacyListener.onPongFrame(payload);
			}
		});
	}
}
