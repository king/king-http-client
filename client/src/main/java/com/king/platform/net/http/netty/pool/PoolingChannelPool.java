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
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class PoolingChannelPool implements ChannelPool {
	private static final Logger logger = getLogger(PoolingChannelPool.class);
	private final long maxTtl;

	private final ConcurrentHashMap<ServerInfo, ServerPool> serverPoolMap = new ConcurrentHashMap<>();

	private final TimeProvider timeProvider;
	private final MetricCallback metricCallback;

	public PoolingChannelPool(final Timer cleanupTimer, TimeProvider timeProvider, long timeoutInMilliseconds, final MetricCallback metricCallback) {
		this.timeProvider = timeProvider;
		this.metricCallback = metricCallback;

		maxTtl = timeoutInMilliseconds;

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

            cleanupTimer.newTimeout(timeout.task(), maxTtl, TimeUnit.MILLISECONDS);

        }, maxTtl, TimeUnit.MILLISECONDS);
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
	public void offer(ServerInfo serverInfo, Channel channel) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			serverPool = new ServerPool(serverInfo, maxTtl, TimeUnit.MILLISECONDS, timeProvider, metricCallback);
			ServerPool old = serverPoolMap.putIfAbsent(serverInfo, serverPool);
			if (old != null) {
				serverPool = old;
			} else {
				metricCallback.onCreatedServerPool(serverInfo.getHost());

			}
		}

		serverPool.offer(channel);
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

	protected int getPoolSize(ServerInfo serverInfo) {
		ServerPool serverPool = serverPoolMap.get(serverInfo);
		if (serverPool == null) {
			return 0;
		}

		return serverPool.getPoolSize();

	}
}
