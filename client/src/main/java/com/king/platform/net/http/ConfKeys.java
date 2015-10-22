// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import com.king.platform.net.http.netty.NettyChannelOptions;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ConfKeys<T> {
	/**
	 * Socket connection timeout in milli seconds
	 */
	public static final ConfKeys<Integer> CONNECT_TIMEOUT_MILLIS = new ConfKeys<>(1000);

	/**
	 * Timeout for idle in milli seconds
	 */
	public static final ConfKeys<Integer> IDLE_TIMEOUT_MILLIS = new ConfKeys<>(1000);

	/**
	 * Total request timeout in milli seconds
	 */
	public static final ConfKeys<Integer> TOTAL_REQUEST_TIMEOUT_MILLIS = new ConfKeys<>(0);


	/**
	 * Should the client accept all certificates from all sources
	 */
	public static final ConfKeys<Boolean> SSL_ALLOW_ALL_CERTIFICATES = new ConfKeys<>(false);

	/**
	 * How long should the client handshake for in milli seconds
	 */
	public static final ConfKeys<Integer> SSL_HANDSHAKE_TIMEOUT_MILLIS = new ConfKeys<>(1000);


	public static final ConfKeys<Integer> HTTP_CODEC_MAX_INITIAL_LINE_LENGTH = new ConfKeys<>(4096);
	public static final ConfKeys<Integer> HTTP_CODEC_MAX_HEADER_SIZE = new ConfKeys<>(8192);
	public static final ConfKeys<Integer> HTTP_CODEC_MAX_CHUNK_SIZE = new ConfKeys<>(1024 * 1024);


	public static final ConfKeys<Boolean> HTTP_FOLLOW_REDIRECTS = new ConfKeys<>(true);


	public static final ConfKeys<Boolean> NETTY_TRACE_LOGS = new ConfKeys<>(false);

	public static final ConfKeys<Boolean> EXECUTE_ON_CALLING_THREAD = new ConfKeys<>(false);


	public static final ConfKeys<NettyChannelOptions> NETTY_CHANNEL_OPTIONS = new ConfKeys<>(new NettyChannelOptions());

	public static final ConfKeys<Boolean> ACCEPT_COMPRESSED_RESPONSE = new ConfKeys<>(false);

	public static final ConfKeys<Boolean> KEEP_ALIVE = new ConfKeys<>(true);

	public static final ConfKeys<Charset> REQUEST_BODY_CHARSET = new ConfKeys<>(StandardCharsets.ISO_8859_1);


	public static final ConfKeys<String> USER_AGENT = new ConfKeys<>("king-http-client");

	private final T defaultValue;

	private ConfKeys(T defaultValue) {
		this.defaultValue = defaultValue;

	}

	public T getDefaultValue() {
		return defaultValue;
	}


}
