// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.Event2;
import com.king.platform.net.http.netty.eventbus.EventBusCallback1;
import com.king.platform.net.http.netty.eventbus.EventBusCallback2;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.eventbus.RootEventBus;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.response.NettyHttpClientResponse;
import com.king.platform.net.http.netty.util.TimeProvider;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ChannelManager {
	private final Logger logger = getLogger(getClass());

	private final EventLoopGroup eventLoopGroup;
	private final TimeProvider timeProvider;
	private final ConfMap confMap;
	private final ChannelPool channelPool;
	private final Bootstrap plainBootstrap;
	private final Bootstrap secureBootstrap;
	private Timer nettyTimer;


	public ChannelManager(EventLoopGroup nioEventLoop, final HttpClientHandler httpClientHandler, Timer nettyTimer, TimeProvider timeProvider, ChannelPool
		channelPool, final ConfMap confMap, RootEventBus rootEventBus) {
		this.eventLoopGroup = nioEventLoop;
		this.nettyTimer = nettyTimer;
		this.timeProvider = timeProvider;
		this.channelPool = channelPool;
		this.confMap = confMap;

		final Class <? extends SocketChannel> socketChannelClass;
		if (Epoll.isAvailable()) {
		    socketChannelClass = EpollSocketChannel.class;
		} else {
		    socketChannelClass = NioSocketChannel.class;
		}

		plainBootstrap = new Bootstrap().channel(socketChannelClass).group(eventLoopGroup);
		plainBootstrap.handler(new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();

				addLoggingIfDesired(pipeline, confMap.get(ConfKeys.NETTY_TRACE_LOGS));
				pipeline.addLast("http-codec", newHttpClientCodec());
				pipeline.addLast("inflater", new HttpContentDecompressor());
				pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
				pipeline.addLast("httpClientHandler", httpClientHandler);

			}
		});


		secureBootstrap = new Bootstrap().channel(socketChannelClass).group(eventLoopGroup);
		secureBootstrap.handler(new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();

				SslInitializer sslInitializer = new SslInitializer(new SSLFactory(confMap.get(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES)), confMap.get(ConfKeys
					.SSL_HANDSHAKE_TIMEOUT_MILLIS));

				pipeline.addLast(SslInitializer.NAME, sslInitializer);
				addLoggingIfDesired(pipeline, confMap.get(ConfKeys.NETTY_TRACE_LOGS));
				pipeline.addLast("http-codec", newHttpClientCodec());
				pipeline.addLast("inflater", new HttpContentDecompressor());
				pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
				pipeline.addLast("httpClientHandler", httpClientHandler);

			}
		});


		secureBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, confMap.get(ConfKeys.CONNECT_TIMEOUT_MILLIS));
		plainBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, confMap.get(ConfKeys.CONNECT_TIMEOUT_MILLIS));

		NettyChannelOptions nettyChannelOptions = confMap.get(ConfKeys.NETTY_CHANNEL_OPTIONS);
		for (ChannelOption channelOption : nettyChannelOptions.keys()) {
			plainBootstrap.option(channelOption, nettyChannelOptions.get(channelOption));
			secureBootstrap.option(channelOption, nettyChannelOptions.get(channelOption));
		}

		rootEventBus.subscribePermanently(Event.ERROR, new ErrorCallback());
		rootEventBus.subscribePermanently(Event.COMPLETED, new CompletedCallback());
		rootEventBus.subscribePermanently(Event.EXECUTE_REQUEST, new ExecuteRequestCallback());
	}

	private void addLoggingIfDesired(ChannelPipeline pipeline, boolean desired) {
		if (desired) {
			pipeline.addLast("logging", new LoggingHandler(LogLevel.TRACE));
		}
	}


	private HttpClientCodec newHttpClientCodec() {
		return new HttpClientCodec(confMap.get(ConfKeys.HTTP_CODEC_MAX_INITIAL_LINE_LENGTH), confMap.get(ConfKeys.HTTP_CODEC_MAX_HEADER_SIZE), confMap.get
			(ConfKeys.HTTP_CODEC_MAX_CHUNK_SIZE));
	}

	public void sendOnChannel(final HttpRequestContext httpRequestContext, RequestEventBus requestEventBus) {

		ServerInfo serverInfo = httpRequestContext.getServerInfo();

		logger.trace("Sending request {} to server {}", httpRequestContext, serverInfo);

		requestEventBus.triggerEvent(Event.onConnecting);

		boolean keepAlive = httpRequestContext.isKeepAlive();

		if (keepAlive && channelPool.isActive()) {
			final Channel channel = channelPool.get(serverInfo);
			if (channel != null) {
				logger.trace("Got old channel {} for request {}", channel, httpRequestContext);
				requestEventBus.triggerEvent(Event.REUSED_CONNECTION, serverInfo);
				requestEventBus.triggerEvent(Event.onConnected);

				sendOnChannel(channel, httpRequestContext, requestEventBus);
			} else {
				logger.trace("Sending on a new channel for request {}", httpRequestContext);
				sendOnNewChannel(httpRequestContext, requestEventBus);
			}
		} else {
			logger.trace("Sending on a new channel for request {}", httpRequestContext);
			sendOnNewChannel(httpRequestContext, requestEventBus);
		}

	}

	private void sendOnChannel(final Channel channel, final HttpRequestContext httpRequestContext, final RequestEventBus requestEventBus) {

		httpRequestContext.attachedToChannel(channel);

		scheduleTimeOutTasks(requestEventBus, httpRequestContext, httpRequestContext.getTotalRequestTimeoutMillis(), httpRequestContext.getIdleTimeoutMillis
			());

		ChannelFuture channelFuture = channel.writeAndFlush(httpRequestContext);
		channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
			@Override
			public void operationComplete(Future<? super Void> future) throws Exception {
				if (!future.isSuccess()) {
					requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, future.cause());
				}
			}
		});

		logger.trace("Wrote {} to channel {}", httpRequestContext, channel);
	}

	private void scheduleTimeOutTasks(RequestEventBus requestEventBus, HttpRequestContext httpRequestContext, int totalRequestTimeoutMillis, int idleTimeoutMillis) {

		if (totalRequestTimeoutMillis > 0) {
			TotalRequestTimeoutTimerTask totalRequestTimeoutTimerTask = new TotalRequestTimeoutTimerTask(requestEventBus, httpRequestContext);
			TimeoutTimerHandler timeoutTimerHandler = new TimeoutTimerHandler(nettyTimer, requestEventBus);
			timeoutTimerHandler.scheduleTimeout(totalRequestTimeoutTimerTask, totalRequestTimeoutMillis, TimeUnit.MILLISECONDS);
		}


		if (idleTimeoutMillis != 0 && (idleTimeoutMillis < totalRequestTimeoutMillis || totalRequestTimeoutMillis == 0)) {
			TimeoutTimerHandler timeoutTimerHandler = new TimeoutTimerHandler(nettyTimer, requestEventBus);
			IdleTimeoutTimerTask idleTimeoutTimerTask = new IdleTimeoutTimerTask(httpRequestContext, timeoutTimerHandler, idleTimeoutMillis,
				totalRequestTimeoutMillis, timeProvider, requestEventBus);
			timeoutTimerHandler.scheduleTimeout(idleTimeoutTimerTask, idleTimeoutMillis, TimeUnit.MILLISECONDS);
		}


	}

	private void sendOnNewChannel(final HttpRequestContext httpRequestContext, final RequestEventBus requestEventBus) {
		final ServerInfo serverInfo = httpRequestContext.getServerInfo();

		ChannelFuture channelFuture = connect(serverInfo);

		channelFuture.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {

					requestEventBus.triggerEvent(Event.CREATED_CONNECTION, serverInfo);

					requestEventBus.triggerEvent(Event.onConnected);

					Channel channel = future.channel();
					channel.attr(ServerInfo.ATTRIBUTE_KEY).set(serverInfo);
					logger.trace("Opened a new channel {}, for request {}", channel, httpRequestContext);
					sendOnChannel(channel, httpRequestContext, requestEventBus);

				} else {
					logger.trace("Failed to opened a new channel for request {}", httpRequestContext);
					Throwable cause = future.cause();
					requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, cause);
				}
			}
		});
	}

	private ChannelFuture connect(ServerInfo serverInfo) {
		Bootstrap bootstrap = plainBootstrap;
		if (serverInfo.isSecure()) {
			bootstrap = secureBootstrap;
			logger.trace("Connecting using secureBootstrap");
		} else {
			logger.trace("Connecting using plainBootstrap");
		}

		return bootstrap.connect(serverInfo.getHost(), serverInfo.getPort());
	}


	private class ErrorCallback implements EventBusCallback2<HttpRequestContext, Throwable> {

		@Override
		public void onEvent(Event2<HttpRequestContext, Throwable> event, HttpRequestContext httpRequestContext, Throwable throwable) {
			ServerInfo serverInfo = httpRequestContext.getServerInfo();

			Channel channel = httpRequestContext.getAndDetachChannel();

			if (channel != null) {
				channelPool.discard(serverInfo, channel);
				channel.close();
			}

			httpRequestContext.getRequestEventBus().triggerEvent(Event.CLOSED_CONNECTION, serverInfo);
		}

	}

	private class CompletedCallback implements EventBusCallback1<HttpRequestContext> {

		@Override
		public void onEvent(Event1<HttpRequestContext> event, HttpRequestContext httpRequestContext) {
			RequestEventBus requestEventBus = httpRequestContext.getRequestEventBus();
			Channel channel = httpRequestContext.getAndDetachChannel();
			ServerInfo serverInfo = httpRequestContext.getServerInfo();

			if (!channelPool.isActive()) {
				channel.close();
				requestEventBus.triggerEvent(Event.CLOSED_CONNECTION, serverInfo);
				return;
			}

			boolean keepAlive = httpRequestContext.isKeepAlive();
			NettyHttpClientResponse nettyHttpClientResponse = httpRequestContext.getNettyHttpClientResponse();
			if (nettyHttpClientResponse == null || nettyHttpClientResponse.getHttpHeaders() == null) {
				keepAlive = false;
			} else {
				String connection = nettyHttpClientResponse.getHttpHeaders().get(HttpHeaders.Names.CONNECTION);
				if (connection != null && HttpHeaders.Values.CLOSE.equalsIgnoreCase(connection)) {
					keepAlive = false;
				}
			}

			if (keepAlive) {
				channelPool.offer(serverInfo, channel);
				requestEventBus.triggerEvent(Event.POOLED_CONNECTION, serverInfo);

			} else {
				channelPool.discard(serverInfo, channel);
				channel.close();
				requestEventBus.triggerEvent(Event.CLOSED_CONNECTION, serverInfo);
			}
		}

	}

	private class ExecuteRequestCallback implements EventBusCallback1<HttpRequestContext> {
		@Override
		public void onEvent(Event1<HttpRequestContext> event, HttpRequestContext httpRequestContext) {
			sendOnChannel(httpRequestContext, httpRequestContext.getRequestEventBus());

		}
	}
}
