// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import io.netty.util.AttributeKey;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

public final class ServerInfo {
	public static final AttributeKey<ServerInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("HttpNetty__ServerInfo");

	private final String scheme;
	private final String host;
	private final int port;

	private final boolean isSecure;
	private final boolean isWebSocket;

	public ServerInfo(String scheme, String host, int port, boolean isSecure, boolean isWebSocket) {
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.isSecure = isSecure;
		this.isWebSocket = isWebSocket;
	}

	public static ServerInfo buildFromUri(String uriString) throws URISyntaxException {
		URI uri = new URI(uriString);

		makeUriUnderscoreCompatible(uri);

		String host = uri.getHost();
		String scheme = uri.getScheme();
		int port = uri.getPort();

		if (host == null) {
			throw new URISyntaxException(uriString, "Host is null");
		}

		if (scheme == null) {
			throw new URISyntaxException(uriString, "Scheme is null");
		}

		scheme = scheme.toLowerCase();

		boolean isSecure = false;

		if ("https".equals(scheme) || "wss".equals(scheme)) {
			isSecure = true;
		}

		if (port < 0) {
			if (isSecure) {
				port = 443;
			} else {
				port = 80;
			}
		}

		boolean isWebSocket = false;
		if ("ws".equals(scheme) || "wss".equals(scheme)) {
			isWebSocket = true;
		}


		return new ServerInfo(scheme, host, port, isSecure, isWebSocket);
	}

	private static void makeUriUnderscoreCompatible(final URI uri) throws URISyntaxException {
		String uriString = uri.toString();

		if (uri.getHost() == null && uriString.contains("_")) {
			try {
				final String hostnameAndPort = uriString.split("/")[2];

				final String semicolon = ":";
				final String hostname = hostnameAndPort.contains(semicolon) ? hostnameAndPort.split(semicolon)[0] : hostnameAndPort;
				final String port = hostnameAndPort.contains(semicolon) ? hostnameAndPort.split(semicolon)[1] : null;

				patchHostname(uri, hostname);
				patchPort(uri, port);

			} catch (NoSuchFieldException | IllegalAccessException | NumberFormatException ex) {
				throw new URISyntaxException(uriString, "Failed to make URI compatible with underscore");
			}
		}
	}

	private static void patchHostname(final URI uri, final String hostname) throws NoSuchFieldException, IllegalAccessException {
		final Field hostField = URI.class.getDeclaredField("host");
		hostField.setAccessible(true);
		hostField.set(uri, hostname);
	}

	private static void patchPort(final URI uri, final String port) throws NoSuchFieldException, IllegalAccessException {
		if (port != null) {
			final Field portField = URI.class.getDeclaredField("port");
			portField.setAccessible(true);
			portField.set(uri, Integer.parseInt(port));
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getScheme() {
		return scheme;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ServerInfo that = (ServerInfo) o;

		if (port != that.port)
			return false;
		if (host != null ? !host.equals(that.host) : that.host != null)
			return false;
		if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = scheme != null ? scheme.hashCode() : 0;
		result = 31 * result + (host != null ? host.hashCode() : 0);
		result = 31 * result + port;
		return result;
	}

	@Override
	public String toString() {
		return "Server {" + scheme + "://" + host + ":" + port + "}";
	}

	public boolean isSecure() {
		return isSecure;
	}

	public boolean isWebSocket() {
		return isWebSocket;
	}
}
