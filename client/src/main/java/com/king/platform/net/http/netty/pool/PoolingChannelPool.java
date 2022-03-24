// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;


import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.channel.Channel;
import io.netty.util.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PoolingChannelPool implements ChannelPool {

	private final ConcurrentHashMap<ServerInfo, ServerPool> serverPoolMap = new ConcurrentHashMap<>();

	private final TimeProvider timeProvider;
	private final MetricCallback metricCallback;

	public PoolingChannelPool(final Timer cleanupTimer, TimeProvider timeProvider, final MetricCallback metricCallback) {
		this.timeProvider = timeProvider;
		this.metricCallback = metricCallback;


		cleanupTimer.newTimeout(timeout -> {

			for (Map.Entry<ServerInfo, ServerPool> poolEntry : serverPoolMap.entrySet()) {
				ServerPool serverPool = poolEntry.getValue();
				ServerInfo serverInfo = poolEntry.getKey();

				serverPool.cleanExpiredConnections();
				if (serverPool.shouldRemovePool()) {
					ServerPool remove = serverPoolMap.remove(serverInfo);
					if (remove != null) {
						metricCallback.onRemovedServerPool(serverInfo.getHost());
					}
				}

			}

			cleanupTimer.newTimeout(timeout.task(), 1000, TimeUnit.MILLISECONDS);

		}, 1000, TimeUnit.MILLISECONDS);
	}


	@Override
	public Channel get(ServerInfo serverInfo) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			return null;
		}

		return serverPool.poll();
	}

	@Override
	public void offer(ServerInfo serverInfo, Channel channel, int keepAliveTimeoutMillis) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			serverPool = new ServerPool(serverInfo, timeProvider, metricCallback);
			ServerPool old = serverPoolMap.putIfAbsent(serverInfo, serverPool);
			if (old != null) {
				serverPool = old;
			} else {
				metricCallback.onCreatedServerPool(serverInfo.getHost());

			}
		}

		serverPool.offer(channel, keepAliveTimeoutMillis);
	}

	@Override
	public void discard(ServerInfo serverInfo, Channel channel) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			return;
		}

		serverPool.discard(channel);
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public void shutdown() {
		for (ServerPool serverPool : serverPoolMap.values()) {
			serverPool.shutdown();
		}
	}

	protected int getPoolSize(ServerInfo serverInfo) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			return 0;
		}

		return serverPool.getPoolSize();

	}
}
