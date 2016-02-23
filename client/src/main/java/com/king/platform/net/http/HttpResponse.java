// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse<T> {
	private List<HeaderParameter> headers = new ArrayList<>();
	private final int statusCode;
	private final ResponseBodyConsumer<T> responseBodyConsumer;


	public HttpResponse(int statusCode, ResponseBodyConsumer<T> responseBodyConsumer, List<Map.Entry<String, String>> httpHeaders) {
		this.statusCode = statusCode;
		this.responseBodyConsumer = responseBodyConsumer;

		for (Map.Entry<String, String> httpHeader : httpHeaders) {
			headers.add(new HeaderParameter(httpHeader.getKey(), httpHeader.getValue()));
		}
	}

	public int getStatusCode() {
		return statusCode;
	}

	public T getBody() {
		return responseBodyConsumer.getBody();
	}

	public String getHeader(String name) {
		for (HeaderParameter header : headers) {
			if (header.getName().equalsIgnoreCase(name)) {
				return header.getValue();
			}
		}
		return null;
	}

	public List<String> getHeaders(String name) {
		List<String> values = new ArrayList<>();

		for (HeaderParameter header : headers) {
			if (header.getName().equalsIgnoreCase(name)) {
				values.add(header.getValue());
			}
		}

		return values;
	}

	public Map<String, String> getAllHeaders() {
		HashMap<String, String> map = new HashMap<>();
		for (HeaderParameter header : headers) {
			map.put(header.getName(), header.getValue());
		}
		return map;
	}


	private static class HeaderParameter {
		private String name;
		private String value;

		public HeaderParameter(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
}
