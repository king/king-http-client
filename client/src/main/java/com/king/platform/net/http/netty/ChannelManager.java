// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.netty.eventbus.*;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.response.NettyHttpClientResponse;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.Timer;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ChannelManager {
	private final Logger logger = getLogger(getClass());

	private final EventLoopGroup eventLoopGroup;
	private final TimeProvider timeProvider;
	private final ConfMap confMap;
	private final ChannelPool channelPool;
	private final Bootstrap bootstrap;
	private final SslContext sslContext;
	private Timer nettyTimer;


	public ChannelManager(EventLoopGroup nioEventLoop, final HttpClientHandler httpClientHandler, Timer nettyTimer, TimeProvider timeProvider, ChannelPool
		channelPool, final ConfMap confMap, RootEventBus rootEventBus) {
		this.eventLoopGroup = nioEventLoop;
		this.nettyTimer = nettyTimer;
		this.timeProvider = timeProvider;
		this.channelPool = channelPool;
		this.confMap = confMap;

		final Class <? extends SocketChannel> socketChannelClass;

		if (Epoll.isAvailable() && confMap.get(ConfKeys.USE_EPOLL)) {
		    socketChannelClass = EpollSocketChannel.class;
		} else {
		    socketChannelClass = NioSocketChannel.class;
		}

		bootstrap = new Bootstrap().channel(socketChannelClass).group(eventLoopGroup);
		bootstrap.handler(new ChannelInitializer() {
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


		sslContext = getSslContext(confMap);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, confMap.get(ConfKeys.CONNECT_TIMEOUT_MILLIS));

		NettyChannelOptions nettyChannelOptions = confMap.get(ConfKeys.NETTY_CHANNEL_OPTIONS);
		for (ChannelOption channelOption : nettyChannelOptions.keys()) {
			bootstrap.option(channelOption, nettyChannelOptions.get(channelOption));
		}

		rootEventBus.subscribePermanently(Event.ERROR, new ErrorCallback());
		rootEventBus.subscribePermanently(Event.COMPLETED, new CompletedCallback());
		rootEventBus.subscribePermanently(Event.EXECUTE_REQUEST, new ExecuteRequestCallback());
	}

	private SslContext getSslContext(ConfMap confMap) {
		SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

		if (confMap.get(ConfKeys.SSL_ALLOW_ALL_CERTIFICATES)) {
			sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
		}

		sslContextBuilder.sessionTimeout(confMap.get(ConfKeys.SSL_HANDSHAKE_TIMEOUT_MILLIS));
		sslContextBuilder.sslProvider(SslProvider.JDK);
		try {
			return sslContextBuilder.build();
		} catch (SSLException e) {
			throw new RuntimeException("Failed to create SslContext", e);
		}

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

		requestEventBus.subscribe(Event.CLOSE, (event, payload) -> channel.close());

		ChannelFuture channelFuture = channel.writeAndFlush(httpRequestContext);
		channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, future.cause());
            }
        });

		logger.trace("Wrote {} to channel {}", httpRequestContext, channel);
	}

	private void scheduleTimeOutTasks(RequestEventBus requestEventBus, HttpRequestContext httpRequestContext, int totalRequestTimeoutMillis, int idleTimeoutMillis) {

		if (totalRequestTimeoutMillis > 0) {
			TotalRequestTimeoutTimerTask totalRequestTimeoutTimerTask = new TotalRequestTimeoutTimerTask(requestEventBus, httpRequestContext);
			TimeoutTimerHandler timeoutTimerHandler = new TimeoutTimerHandler(nettyTimer, requestEventBus, totalRequestTimeoutTimerTask);
			timeoutTimerHandler.scheduleTimeout(totalRequestTimeoutMillis, TimeUnit.MILLISECONDS);
		}


		if (idleTimeoutMillis != 0 && (idleTimeoutMillis < totalRequestTimeoutMillis || totalRequestTimeoutMillis == 0)) {
			IdleTimeoutTimerTask idleTimeoutTimerTask = new IdleTimeoutTimerTask(httpRequestContext, idleTimeoutMillis,
				totalRequestTimeoutMillis, timeProvider, requestEventBus);

			TimeoutTimerHandler timeoutTimerHandler = new TimeoutTimerHandler(nettyTimer, requestEventBus, idleTimeoutTimerTask);
			timeoutTimerHandler.scheduleTimeout(idleTimeoutMillis, TimeUnit.MILLISECONDS);
			idleTimeoutTimerTask.setTimeoutTimerHandler(timeoutTimerHandler);

		}

	}

	private void sendOnNewChannel(final HttpRequestContext httpRequestContext, final RequestEventBus requestEventBus) {
		final ServerInfo serverInfo = httpRequestContext.getServerInfo();

		ChannelFuture channelFuture = bootstrap.connect(serverInfo.getHost(), serverInfo.getPort());

		channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {

                requestEventBus.triggerEvent(Event.CREATED_CONNECTION, serverInfo);
                requestEventBus.triggerEvent(Event.onConnected);

                Channel channel = future.channel();
				logger.trace("Opened a new channel {}, for request {}", channel, httpRequestContext);
				channel.attr(ServerInfo.ATTRIBUTE_KEY).set(serverInfo);

                if (serverInfo.isSecure()) {
					SslHandler sslHandler = sslContext.newHandler(channel.alloc(), serverInfo.getHost(), serverInfo.getPort());
					channel.pipeline().addFirst("ssl", sslHandler);

					sslHandler.handshakeFuture().addListener((FutureListener<Channel>) sslHandshakeFuture -> {
                        if (sslHandshakeFuture.isSuccess()) {
							logger.trace("SSL handshake successful, sending on channel {}, for request {}", channel, httpRequestContext);
                            sendOnChannel(channel, httpRequestContext, requestEventBus);
                        } else {
                            logger.error("Failed to do ssl handshake");
                            Throwable cause = sslHandshakeFuture.cause();
                            requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, cause);
                        }
                    });

				} else {
					logger.trace("Sending over clear channel channel {}, for request {}", channel, httpRequestContext);
					sendOnChannel(channel, httpRequestContext, requestEventBus);
				}

            } else {
                logger.trace("Failed to opened a new channel for request {}", httpRequestContext);
                Throwable cause = future.cause();
                requestEventBus.triggerEvent(Event.ERROR, httpRequestContext, cause);
            }
        });
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
				if (channel != null) {
					channel.close();
				}
				requestEventBus.triggerEvent(Event.CLOSED_CONNECTION, serverInfo);
				return;
			}

			boolean keepAlive = httpRequestContext.isKeepAlive();
			NettyHttpClientResponse nettyHttpClientResponse = httpRequestContext.getNettyHttpClientResponse();
			if (nettyHttpClientResponse == null || nettyHttpClientResponse.getHttpHeaders() == null) {
				keepAlive = false;
			} else {
				String connection = nettyHttpClientResponse.getHttpHeaders().get(HttpHeaderNames.CONNECTION);
				if (connection != null && HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(connection)) {
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
