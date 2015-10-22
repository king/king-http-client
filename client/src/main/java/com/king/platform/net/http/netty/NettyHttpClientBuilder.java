// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.netty.backpressure.BackPressure;
import com.king.platform.net.http.netty.backpressure.NoBackPressure;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.RootEventBus;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.metric.MetricCollector;
import com.king.platform.net.http.netty.metric.RecordedTimeStamps;
import com.king.platform.net.http.netty.pool.ChannelPool;
import com.king.platform.net.http.netty.pool.PoolingChannelPool;
import com.king.platform.net.http.netty.util.SystemTimeProvider;
import com.king.platform.net.http.netty.util.TimeProvider;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NettyHttpClientBuilder {
	private int nioThreads = 2;
	private int httpCallbackExecutorThreads;
	private int httpExecuteExecutorThreads;


	private ThreadFactory nioThreadFactory;
	private Executor httpCallbackExecutor;
	private Executor httpExecuteExecutor;

	private Timer cleanupTimer;
	private TimeProvider timeProvider;
	private RootEventBus rootEventBus;
	private BackPressure executionBackPressure;
	private ChannelPool channelPool;

	private MetricCallback metricCallback;

	public NettyHttpClientBuilder setNioThreads(int nioThreads) {
		this.nioThreads = nioThreads;
		return this;
	}

	public NettyHttpClientBuilder setMetricCallback(MetricCallback metricCallback) {
		this.metricCallback = metricCallback;
		return this;
	}

	public NettyHttpClientBuilder setHttpCallbackExecutorThreads(int httpCallbackExecutorThreads) {
		if (httpCallbackExecutor != null) {
			throw new IllegalStateException("Can't set callback dispatcher threads when httpCallbackExecutor has already been set.");
		}
		this.httpCallbackExecutorThreads = httpCallbackExecutorThreads;
		return this;
	}

	public NettyHttpClientBuilder setHttpCallbackExecutor(Executor executor) {
		if (httpCallbackExecutorThreads != 0) {
			throw new IllegalStateException("Can't set httpCallbackExecutor when httpCallbackExecutorThreads has already been set.");
		}

		this.httpCallbackExecutor = executor;
		return this;
	}


	public NettyHttpClientBuilder setHttpExecuteExecutorThreads(int httpExecuteExecutorThreads) {
		if (httpExecuteExecutor != null) {
			throw new IllegalStateException("Can't set httpExecuteExecutorThreads  when httpExecuteExecutor has already been set.");
		}
		this.httpExecuteExecutorThreads = httpExecuteExecutorThreads;
		return this;
	}

	public NettyHttpClientBuilder setHttpExecuteExecutor(Executor httpExecuteExecutor) {
		if (httpExecuteExecutorThreads != 0) {
			throw new IllegalStateException("Can't set httpExecuteExecutor when httpExecuteExecutorThreads has already been set.");
		}

		this.httpExecuteExecutor = httpExecuteExecutor;
		return this;
	}


	public NettyHttpClientBuilder setNioThreadFactory(ThreadFactory nioThreadFactory) {
		this.nioThreadFactory = nioThreadFactory;
		return this;
	}


	public NettyHttpClientBuilder setCleanupTimer(Timer cleanupTimer) {
		this.cleanupTimer = cleanupTimer;
		return this;
	}

	public NettyHttpClientBuilder setTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
		return this;
	}

	public NettyHttpClientBuilder setExecutionBackPressure(BackPressure executionBackPressure) {
		this.executionBackPressure = executionBackPressure;
		return this;
	}

	public NettyHttpClientBuilder setRootEventBus(RootEventBus rootEventBus) {
		this.rootEventBus = rootEventBus;
		return this;
	}

	public NettyHttpClientBuilder setChannelPool(ChannelPool channelPool) {
		this.channelPool = channelPool;
		return this;
	}

	public NettyHttpClient createHttpClient() {
		if (httpCallbackExecutor == null) {
			if (httpCallbackExecutorThreads == 0) {
				httpCallbackExecutorThreads = 2;
			}
			httpCallbackExecutor = Executors.newFixedThreadPool(httpCallbackExecutorThreads, newThreadFactory("HttpClient-HttpCallback"));
		}

		if (httpExecuteExecutor == null) {
			if (httpExecuteExecutorThreads == 0) {
				httpExecuteExecutorThreads = 2;
			}
			httpExecuteExecutor = Executors.newFixedThreadPool(httpExecuteExecutorThreads, newThreadFactory("HttpClient-Executor"));
		}

		if (nioThreadFactory == null) {
			this.nioThreadFactory = newThreadFactory("HttpClient nio event loop");
		}

		if (cleanupTimer == null) {
			cleanupTimer = new HashedWheelTimer();
		}

		if (timeProvider == null) {
			timeProvider = new SystemTimeProvider();
		}

		if (rootEventBus == null) {
			rootEventBus = new DefaultEventBus();
		}

		if (metricCallback == null) {
			metricCallback = EMPTY_METRIC_CALLBACK;
		} else {
			new MetricCollector().wireMetricCallbackOnEventBus(metricCallback, rootEventBus);
		}

		if (channelPool == null) {
			channelPool = new PoolingChannelPool(cleanupTimer, timeProvider, 30, metricCallback);
		}

		if (executionBackPressure == null) {
			executionBackPressure = new NoBackPressure();
		}

		return new NettyHttpClient(nioThreads, nioThreadFactory, httpCallbackExecutor, httpExecuteExecutor, cleanupTimer, timeProvider, executionBackPressure,
			rootEventBus, channelPool);
	}


	private ThreadFactory newThreadFactory(final String name) {
		return new ThreadFactory() {
			private int threadId = 0;

			@Override
			public Thread newThread(Runnable runnable) {
				Thread t = new Thread(runnable, name + " " + ++threadId);
				t.setDaemon(true);
				return t;
			}
		};
	}

	private final static MetricCallback EMPTY_METRIC_CALLBACK = new MetricCallback() {
		@Override
		public void onClosedConnectionTo(String host) {

		}

		@Override
		public void onCreatedConnectionTo(String host) {

		}

		@Override
		public void onReusedConnectionTo(String host) {

		}

		@Override
		public void onError(String host, RecordedTimeStamps recordedTimeStamps) {

		}

		@Override
		public void onCompletedRequest(String host, RecordedTimeStamps recordedTimeStamps) {

		}

		@Override
		public void onCreatedServerPool(String host) {

		}

		@Override
		public void onRemovedServerPool(String host) {

		}

		@Override
		public void onServerPoolClosedConnection(String host, int poolSize) {

		}

		@Override
		public void onServerPoolAddedConnection(String host, int poolSize) {

		}
	};
}
