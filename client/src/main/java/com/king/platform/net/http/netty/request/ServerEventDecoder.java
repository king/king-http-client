package com.king.platform.net.http.netty.request;


import com.king.platform.net.http.HttpSseCallback;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class ServerEventDecoder {
	private final HttpSseCallback httpSseCallback;
	private final Executor httpClientCallbackExecutor;

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder data = new StringBuilder();

	private String lastEventId;
	private String eventName;


	public ServerEventDecoder(HttpSseCallback httpSseCallback, Executor httpClientCallbackExecutor) {
		this.httpSseCallback = httpSseCallback;
		this.httpClientCallbackExecutor = httpClientCallbackExecutor;

	}

	public void reset() {
		buffer.setLength(0);
		data.setLength(0);
		lastEventId = null;
		eventName = null;
	}

	public void onReceivedContentPart(ByteBuf content) {
		String contentString = content.toString(StandardCharsets.UTF_8);
		try {
			char[] chars = contentString.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				char c = chars[i];

				if (isCRLF(i, chars)) {
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
			throw new RuntimeException(e);
		}
	}

	private boolean isCRLF(int index, char[] buffer) {
		if (buffer[index] != '\r') {
			return false;
		}

		if (index + 1 >= buffer.length) {
			return false;
		}

		return buffer[index+1] == '\n';
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

	private void dispatchEvent() {
		String thisEventName = eventName;
		eventName = null;

		if (data.length() == 0) {
			return;
		}

		if (isNewLine(data.charAt(data.length()-1))) {
			data.setLength(data.length() - 1);
		}

		String dataString = data.toString();
		data.setLength(0);

		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				httpSseCallback.onEvent(lastEventId, thisEventName, dataString);
			}
		});

	}

	private boolean isNewLine(char c) {
		return c == '\r' || c == '\n';
	}


}
