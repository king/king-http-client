// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;


import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.channel.Channel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public class ServerPool {
	private final Logger logger = getLogger(getClass());
	private final ServerInfo server;

	private final AtomicInteger idGenerator = new AtomicInteger();
	private final ConcurrentLinkedDeque<PooledChannel> pooledChannels = new ConcurrentLinkedDeque<>();
	private final ConcurrentHashMap<Channel, PooledChannel> channelsMap = new ConcurrentHashMap<>();
	private final TimeProvider timeProvider;
	private final MetricCallback metricCallback;

	private volatile long lastOfferedConnectionTime;

	public ServerPool(ServerInfo server,TimeProvider timeProvider, MetricCallback metricCallback) {
		this.timeProvider = timeProvider;
		this.metricCallback = metricCallback;
		lastOfferedConnectionTime = timeProvider.currentTimeInMillis();
		this.server = server;
	}

	public Channel poll() {
		PooledChannel pooledChannel;

		while ((pooledChannel = pooledChannels.poll()) != null) {
			if (isValidConnection(pooledChannel)) {
				logger.trace("Found active channel for server {} with id {} created at {}", server, pooledChannel.id, pooledChannel.creationTimeStamp);
				return pooledChannel.channel;
			} else {
				channelsMap.remove(pooledChannel.channel);
				pooledChannel.channel.close();
				logger.trace("Channel to {} with id {} created at {} is dead!", server, pooledChannel.id, pooledChannel.creationTimeStamp);
				metricCallback.onServerPoolClosedConnection(server.getHost(), channelsMap.size());
			}
		}

		return null;

	}

	private boolean isValidConnection(PooledChannel pooledChannel) {
		if (pooledChannel == null) {
			return false;
		}

		Channel channel = pooledChannel.channel;
		long currentTime = timeProvider.currentTimeInMillis();

		if (pooledChannel.lastUsedTimeStamp + pooledChannel.maxTTL <= currentTime) {  //TTL for this connection has expired
			return false;
		}

		if (channel.isActive() && channel.isOpen()) {
			return true;
		}

		return false;
	}

	public void offer(Channel channel, int keepAliveTimeoutMillis) {
		if (channel == null) {
			return;
		}

		PooledChannel pooledChannel = channelsMap.get(channel);

		if (!channel.isActive() || !channel.isOpen()) {
			if (pooledChannel == null) {
				return;
			}

			channelsMap.remove(channel);
			return;
		}

		if (pooledChannel == null) {
			pooledChannel = new PooledChannel(idGenerator.incrementAndGet(), timeProvider.currentTimeInMillis(), channel, keepAliveTimeoutMillis);
			PooledChannel oldValue = channelsMap.putIfAbsent(channel, pooledChannel);
			if (oldValue == null) {
				logger.trace("Adding new active channel for server {} with id {} created at {}", server, pooledChannel.id, pooledChannel.creationTimeStamp);
				metricCallback.onServerPoolAddedConnection(server.getHost(), channelsMap.size());
			}

		}
		logger.trace("offering active channel for server {} with id {} created at {}", server, pooledChannel.id, pooledChannel.creationTimeStamp);

		lastOfferedConnectionTime = timeProvider.currentTimeInMillis();
		pooledChannel.lastUsedTimeStamp = timeProvider.currentTimeInMillis();
		pooledChannels.addFirst(pooledChannel);
	}

	public void discard(Channel channel) {
		if (channel == null) {
			return;
		}

		channel.close();
		PooledChannel remove = channelsMap.remove(channel);
		if (remove != null) {
			//pooledChannels.remove(remove); we don't want to do remove since its expensive on a linkedList
			//discard should not log an metric call for this since an event is triggerd by the calling method
			//metricCallback.onServerPoolClosedConnection(server.getHost(), channelsMap.size());
		}

	}

	public void cleanExpiredConnections() {
		HashSet<PooledChannel> currentPooledChannels = new HashSet<>(pooledChannels);

		List<PooledChannel> closedChannels = new ArrayList<>();
		for (PooledChannel pooledChannel : currentPooledChannels) {
			if (!isValidConnection(pooledChannel)) {
				closedChannels.add(pooledChannel);
			}
		}

		for (PooledChannel closedChannel : closedChannels) {
			pooledChannels.remove(closedChannel);
			PooledChannel remove = channelsMap.remove(closedChannel.channel);

			logger.trace("Cleaned expired connection {}", closedChannel.channel);
			if (remove != null) {
				remove.channel.close();
				metricCallback.onServerPoolClosedConnection(server.getHost(), channelsMap.size());
			}

		}


		for (Channel channel : channelsMap.keySet()) {
			if (channel.isActive() && channel.isOpen()) {
				continue;
			}

			channelsMap.remove(channel);
			channel.close();
			metricCallback.onServerPoolClosedConnection(server.getHost(), channelsMap.size());
		}
	}


	public boolean shouldRemovePool() {
		return pooledChannels.isEmpty() && lastOfferedConnectionTime + 10_000 <= timeProvider.currentTimeInMillis();  //keep the object around for 10s after the last connection has been closed
	}

	public int getPoolSize() {
		return pooledChannels.size();
	}

	public int getChannelSize() {
		return channelsMap.size();
	}

	public void shutdown() {
		for (PooledChannel pooledChannel : pooledChannels) {
			pooledChannel.channel.close().awaitUninterruptibly();
		}
	}


	private static class PooledChannel {
		private int id;
		private long creationTimeStamp;
		private long lastUsedTimeStamp;
		private Channel channel;
		private int maxTTL;

		public PooledChannel(int id, long creationTimeStamp, Channel channel, int maxTTL) {
			this.id = id;
			this.creationTimeStamp = creationTimeStamp;
			this.channel = channel;
			this.maxTTL = maxTTL;
		}
	}
}
