package com.king.platform.net.http.netty;

import com.king.platform.net.http.Headers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NettyHeaders implements Headers {
	private final io.netty.handler.codec.http.HttpHeaders httpHeaders;

	public NettyHeaders(io.netty.handler.codec.http.HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	@Override public String get(CharSequence name) {
		return httpHeaders.get(name);
	}

	@Override public List<String> getAll(CharSequence name) {
		return httpHeaders.getAll(name);
	}

	@Override public List<Map.Entry<String, String>> entries() {
		return httpHeaders.entries();
	}

	@Override public boolean contains(CharSequence name) {
		return httpHeaders.contains(name);
	}

	@Override public int size() {
		return httpHeaders.size();
	}

	@Override public Set<String> names() {
		return httpHeaders.names();
	}

	@Override public Iterator<Map.Entry<String, String>> iterator() {
		return httpHeaders.iteratorAsString();
	}
}
