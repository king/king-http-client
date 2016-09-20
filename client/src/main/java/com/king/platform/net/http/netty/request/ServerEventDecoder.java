package com.king.platform.net.http.netty.request;


import com.king.platform.net.http.HttpSSECallback;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class ServerEventDecoder {
	private final HttpSSECallback httpSSECallback;
	private final Executor httpClientCallbackExecutor;

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder data = new StringBuilder();

	private String lastEventId;
	private String eventName;


	public ServerEventDecoder(HttpSSECallback httpSSECallback, Executor httpClientCallbackExecutor) {
		this.httpSSECallback = httpSSECallback;
		this.httpClientCallbackExecutor = httpClientCallbackExecutor;

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
		if (isNewLine(data.charAt(data.length()-1))) {
			data.setLength(data.length() - 1);
		}

		String dataString = data.toString();
		data.setLength(0);

		httpClientCallbackExecutor.execute(new Runnable() {
			@Override
			public void run() {
				httpSSECallback.onEvent(lastEventId, eventName, dataString);
			}
		});

	}

	private boolean isNewLine(char c) {
		return c == '\r' || c == '\n';
	}


}
