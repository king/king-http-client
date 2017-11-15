// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import io.netty.util.AttributeKey;

import java.net.URI;
import java.net.URISyntaxException;

public class ServerInfo {
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
		String host = uri.getHost();
		String scheme = uri.getScheme();
		int port = uri.getPort();

		if (host == null) {
			throw new URISyntaxException(uriString, "Host is null");
		}

		if (scheme == null) {
			throw new URISyntaxException(uriString, "Scheme is null");
		}


		if (port < 0) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}
		boolean isSecure= false;

		if ("https".equalsIgnoreCase(scheme)  || "wss".equalsIgnoreCase(scheme)) {
				isSecure = true;
		}

		boolean isWebSocket = false;
		if ("ws".equalsIgnoreCase(scheme)  || "wss".equalsIgnoreCase(scheme)) {
			isWebSocket = true;
		}


		return new ServerInfo(scheme, host, port, isSecure, isWebSocket);
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


