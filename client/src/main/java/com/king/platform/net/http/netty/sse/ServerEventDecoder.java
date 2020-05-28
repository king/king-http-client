// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.KingHttpException;
import com.king.platform.net.http.SseClientCallback;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class ServerEventDecoder {
	private final SseClientCallback sseClientCallback;

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder data = new StringBuilder();

	private String lastEventId;
	private String eventName;


	public ServerEventDecoder(SseClientCallback sseClientCallback) {
		this.sseClientCallback = sseClientCallback;

	}

	public void reset() {
		buffer.setLength(0);
		data.setLength(0);
		lastEventId = null;
		eventName = null;
	}

	public void onReceivedContentPart(ByteBuf content) throws KingHttpException {
		String contentString = content.toString(StandardCharsets.UTF_8);
		try {
			char[] chars = contentString.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				char c = chars[i];

				if (chars[i] == '\r') {
					// Ignore CR
					continue;
				}

				if (isNewLine(c)) {
					String line = buffer.toString();
					buffer.setLength(0);
					parseLine(line);
				} else {
					buffer.append(c);
				}

			}
		} catch (Exception e) {
			throw new KingHttpException("Failed to parse incoming content in SSE stream", e);
		}
	}

	private boolean isNewLine(char c) {
		return c == '\r' || c == '\n';
	}

	private void parseLine(String line) {
		line = line.trim();

		if (line.isEmpty()) {
			dispatchEvent();
			return;
		}

		if (line.startsWith(":")) {
			return;
		}

		int colonIndex;

		if ((colonIndex = line.indexOf(":")) != -1) {
			String field = line.substring(0, colonIndex);
			String value;
			if (line.charAt(colonIndex + 1) == ' ') {
				value = line.substring(colonIndex + 2);
			} else {
				value = line.substring(colonIndex + 1);
			}
			processField(field, value);
		} else {
			processField(line.trim(), "");
		}


	}

	private void dispatchEvent() {
		final String thisEventName = eventName;
		eventName = null;

		if (data.length() == 0) {
			return;
		}

		if (isNewLine(data.charAt(data.length() - 1))) {
			data.setLength(data.length() - 1);
		}

		final String dataString = data.toString();
		data.setLength(0);

		sseClientCallback.onEvent(lastEventId, thisEventName, dataString);
	}

	private void processField(String field, String value) {
		switch (field.charAt(0)) {
			case 'd': {
				data.append(value).append("\n");
				break;
			}

			case 'i': {
				lastEventId = value;
				break;
			}

			case 'e': {
				eventName = value;
				break;
			}

			case 'r': {  //handle retry values from the server
				break;
			}
		}
	}


}
