package com.king.platform.net.http;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Headers {
	private final io.netty.handler.codec.http.HttpHeaders httpHeaders;

	public Headers(io.netty.handler.codec.http.HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public String get(String name) {
		return httpHeaders.get(name);
	}

	public List<String> getAll(String name) {
		return httpHeaders.getAll(name);
	}

	public List<Map.Entry<String, String>> entries() {
		return httpHeaders.entries();
	}

	public boolean contains(String name) {
		return httpHeaders.contains(name);
	}

	public int size() {
		return httpHeaders.size();
	}

	public Set<String> names() {
		return httpHeaders.names();
	}

	public Iterator<Map.Entry<String, String>> iterator() {
		return httpHeaders.iteratorAsString();
	}
}
