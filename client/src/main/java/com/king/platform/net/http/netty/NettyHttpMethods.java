package com.king.platform.net.http.netty;

import com.king.platform.net.http.HttpMethod;

import java.util.EnumMap;
import java.util.Map;

final class NettyHttpMethods {

	private static final Map<HttpMethod, io.netty.handler.codec.http.HttpMethod> mappings;

	static {
		mappings = new EnumMap<>(HttpMethod.class);
		mappings.put(HttpMethod.GET, io.netty.handler.codec.http.HttpMethod.GET);
		mappings.put(HttpMethod.POST, io.netty.handler.codec.http.HttpMethod.POST);
		mappings.put(HttpMethod.PUT, io.netty.handler.codec.http.HttpMethod.PUT);
		mappings.put(HttpMethod.DELETE, io.netty.handler.codec.http.HttpMethod.DELETE);
		mappings.put(HttpMethod.HEAD, io.netty.handler.codec.http.HttpMethod.HEAD);
		mappings.put(HttpMethod.OPTIONS, io.netty.handler.codec.http.HttpMethod.OPTIONS);
		mappings.put(HttpMethod.TRACE, io.netty.handler.codec.http.HttpMethod.TRACE);

		verifyIntegrity();
	}

	private NettyHttpMethods() {
	}

	static io.netty.handler.codec.http.HttpMethod toNettyMethod(final HttpMethod httpMethod) {
		return mappings.get(httpMethod);
	}

	private static void verifyIntegrity() {
		for (final HttpMethod httpMethod : HttpMethod.values()) {
			if (!mappings.containsKey(httpMethod)) {
				throw new IllegalStateException(
					"Missing mapping to Netty HttpMethod for " + httpMethod.getDeclaringClass().getName() + "." + httpMethod);
			}
		}
	}

}
