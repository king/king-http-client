// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import com.king.platform.net.http.netty.NettyChannelOptions;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ConfKeys<T> {
	/**
	 * Socket connection timeout in milli seconds, defaults to 1000
	 */
	public static final ConfKeys<Integer> CONNECT_TIMEOUT_MILLIS = new ConfKeys<>(1000);

	/**
	 * Timeout for idle in milli seconds, defaults to 1000
	 */
	public static final ConfKeys<Integer> IDLE_TIMEOUT_MILLIS = new ConfKeys<>(1000);

	/**
	 * Total request timeout in milli seconds, defaults to 0
	 */
	public static final ConfKeys<Integer> TOTAL_REQUEST_TIMEOUT_MILLIS = new ConfKeys<>(0);


	/**
	 * Should the client accept all certificates from all sources, defaults to false
	 */
	public static final ConfKeys<Boolean> SSL_ALLOW_ALL_CERTIFICATES = new ConfKeys<>(false);

	/**
	 * How long should the client handshake for in milli seconds, defaults to 1000
	 */
	public static final ConfKeys<Integer> SSL_HANDSHAKE_TIMEOUT_MILLIS = new ConfKeys<>(1000);


	/**
	 * Set the max initial line length for the http codec, defaults to 4096
	 */
	public static final ConfKeys<Integer> HTTP_CODEC_MAX_INITIAL_LINE_LENGTH = new ConfKeys<>(4096);

	/**
	 * Set the max header size for http codec, defaults to 8192
	 */
	public static final ConfKeys<Integer> HTTP_CODEC_MAX_HEADER_SIZE = new ConfKeys<>(8192);

	/**
	 * Set the chunk size for chunked reading in the http codec, defaults to 1024*1024
	 */
	public static final ConfKeys<Integer> HTTP_CODEC_MAX_CHUNK_SIZE = new ConfKeys<>(1024 * 1024);


	/**
	 * Should the client follow http redirects, defaults to true
	 */
	public static final ConfKeys<Boolean> HTTP_FOLLOW_REDIRECTS = new ConfKeys<>(true);


	/**
	 * Should netty log everything to trace logs, defaults to false
	 */
	public static final ConfKeys<Boolean> NETTY_TRACE_LOGS = new ConfKeys<>(false);

	/**
	 * Should the client execute (start) requests on the calling thread, defaults to false
	 */
	public static final ConfKeys<Boolean> EXECUTE_ON_CALLING_THREAD = new ConfKeys<>(false);


	/**
	 * Use epoll if available
	 */
	public static final ConfKeys<Boolean> USE_EPOLL = new ConfKeys<>(false);


	/**
	 * Use custom dns resolver instead of standard {@link io.netty.resolver.DefaultAddressResolverGroup}
	 */
	public static final ConfKeys<AddressResolverGroup<?>> DNS_RESOLVER = new ConfKeys<>(DefaultAddressResolverGroup.INSTANCE);

	/**
	 * Add any custom {@link io.netty.channel.ChannelOption} that will be used to populate the connection to the servers.
	 */
	public static final ConfKeys<NettyChannelOptions> NETTY_CHANNEL_OPTIONS = new ConfKeys<>(new NettyChannelOptions());

	/**
	 * Should the client accept compressed responses, defaults to false
	 */
	public static final ConfKeys<Boolean> ACCEPT_COMPRESSED_RESPONSE = new ConfKeys<>(false);

	/**
	 * Should the client automatically decompress responses, defaults to true
	 */
	public static final ConfKeys<Boolean> AUTOMATICALLY_DECOMPRESS_RESPONSE = new ConfKeys<>(true);

	/**
	 * Should the client keep the connections alive between reqCuests, defaults to true
	 */
	public static final ConfKeys<Boolean> KEEP_ALIVE = new ConfKeys<>(true);

	/**
	 * Set default request body charset, defaults to iso-8859-1
	 */
	public static final ConfKeys<Charset> REQUEST_BODY_CHARSET = new ConfKeys<>(StandardCharsets.ISO_8859_1);


	/**
	 * Set user agent for the client, defaults to king-http-client
	 */
	public static final ConfKeys<String> USER_AGENT = new ConfKeys<>("king-http-client");


	/**
	 * Maximum frame size for an websocket frame. This size should be negotiated between server and client.
	 */
	@Deprecated
	public static final ConfKeys<Integer> WEB_SOCKET_MAX_FRAME_SIZE = new ConfKeys<>(32*1024);

	public static final ConfKeys<Integer> WEB_SOCKET_MAX_INCOMING_FRAME_SIZE = new ConfKeys<>(32*1024);

	public static final ConfKeys<Integer> WEB_SOCKET_MAX_OUTGOING_FRAME_SIZE = new ConfKeys<>(32*1024);

	/**
	 * Should incoming websocket continuation frames be aggregated automatically?
	 */
	public static final ConfKeys<Boolean> WEB_SOCKET_AGGREGATE_FRAMES = new ConfKeys<>(true);

	/**
	 * Maximum aggregated size of incoming websocket frames
	 */
	public static final ConfKeys<Integer> WEB_SOCKET_MAX_AGGREGATE_BUFFER_SIZE = new ConfKeys<>(128 * 1024);

	/**
	 * Should outgoing websocket frames be split up into continuation frames if they are larger then {@link #WEB_SOCKET_MAX_FRAME_SIZE}
	 * @deprecated
	 */
	@Deprecated
	public static final ConfKeys<Boolean> WEB_SOCKET_SPLIT_FRAMES = new ConfKeys<>(true);

	/**
	 * Should web-socket client automatically echo back pong on ping frames
	 */
	public static final ConfKeys<Boolean> WEB_SOCKET_AUTO_PONG = new ConfKeys<>(true);

	/**
	 * Should the web-socket client send back an close frame
	 */
	public static final ConfKeys<Boolean> WEB_SOCKET_AUTO_CLOSE_FRAME = new ConfKeys<>(true);


	private final T defaultValue;

	private ConfKeys(T defaultValue) {
		this.defaultValue = defaultValue;

	}

	public T getDefaultValue() {
		return defaultValue;
	}


}
