// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.HttpClient;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Builder for creating a Netty implementation of HttpClient.
 *
 * <p>Example on how to use this builder.
 * <pre>{@code
 * NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder();
 * HttpClient httpClient = nettyHttpClientBuilder.createHttpClient();
 * httpClient.start();
 * }</pre>
 */
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
	private int keepAliveTimeoutMS = 30_000;

	/**
	 * Set the amount of nio threads used for netty io reactor - defaults to two.
	 * @param nioThreads the number of nio threads
	 * @return the builder
	 */
	public NettyHttpClientBuilder setNioThreads(int nioThreads) {
		this.nioThreads = nioThreads;
		return this;
	}

	/**
	 * Set an metric callback which can be used to collect metrics of each calls and the state of the client.
	 * @param metricCallback the metric callback implementation
	 * @return the builder
	 */
	public NettyHttpClientBuilder setMetricCallback(MetricCallback metricCallback) {
		this.metricCallback = metricCallback;
		return this;
	}

	/**
	 * Set the amount of threads used for http callbacks. Defaults to two.
	 * Can only be set if httpCallbackExecutor has not been set.
	 * @param httpCallbackExecutorThreads the number of threads
	 * @return the builder
	 */
	public NettyHttpClientBuilder setHttpCallbackExecutorThreads(int httpCallbackExecutorThreads) {
		if (httpCallbackExecutor != null) {
			throw new IllegalStateException("Can't set callback dispatcher threads when httpCallbackExecutor has already been set.");
		}
		this.httpCallbackExecutorThreads = httpCallbackExecutorThreads;
		return this;
	}

	/**
	 * Set an custom executor used for http callbacks.
	 * Can only be set if httpCallbackExecutorThreads has not been set.
	 * @param executor the executor used for callbacks
	 * @return the builder
	 */
	public NettyHttpClientBuilder setHttpCallbackExecutor(Executor executor) {
		if (httpCallbackExecutorThreads != 0) {
			throw new IllegalStateException("Can't set httpCallbackExecutor when httpCallbackExecutorThreads has already been set.");
		}

		this.httpCallbackExecutor = executor;
		return this;
	}


	/**
	 * Set the amount of threads used for executor used for executing http requests. Defaults to two.
	 * Not used if {@link com.king.platform.net.http.ConfKeys#EXECUTE_ON_CALLING_THREAD} is set to true.
	 * Can only be set if httpExecuteExecutor has not been set.
	 * @param httpExecuteExecutorThreads the amount of threads
	 * @return the builder
	 */
	public NettyHttpClientBuilder setHttpExecuteExecutorThreads(int httpExecuteExecutorThreads) {
		if (httpExecuteExecutor != null) {
			throw new IllegalStateException("Can't set httpExecuteExecutorThreads  when httpExecuteExecutor has already been set.");
		}
		this.httpExecuteExecutorThreads = httpExecuteExecutorThreads;
		return this;
	}

	/**
	 * Set a custom executor used for executing http requests.
	 * Not used if {@link com.king.platform.net.http.ConfKeys#EXECUTE_ON_CALLING_THREAD} is set to true.
	 * Can only be set if httpExecuteExecutorThreads has not been set.
	 * @param httpExecuteExecutor the executor used for executing requests
	 * @return the builder
	 */
	public NettyHttpClientBuilder setHttpExecuteExecutor(Executor httpExecuteExecutor) {
		if (httpExecuteExecutorThreads != 0) {
			throw new IllegalStateException("Can't set httpExecuteExecutor when httpExecuteExecutorThreads has already been set.");
		}

		this.httpExecuteExecutor = httpExecuteExecutor;
		return this;
	}


	/**
	 * Set an custom thread factory for netty nio.
	 * @param nioThreadFactory the thread factory.
	 * @return the builder
	 */
	public NettyHttpClientBuilder setNioThreadFactory(ThreadFactory nioThreadFactory) {
		this.nioThreadFactory = nioThreadFactory;
		return this;
	}


	/**
	 * Set a custom timer used for cleanup jobs
	 * @param cleanupTimer the cleanup timer
	 * @return the builder
	 */
	public NettyHttpClientBuilder setCleanupTimer(Timer cleanupTimer) {
		this.cleanupTimer = cleanupTimer;
		return this;
	}


	/**
	 * Set a custom timeProvider implementation
	 * @param timeProvider the time provider
	 * @return the builder
	 */
	public NettyHttpClientBuilder setTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
		return this;
	}

	/**
	 * Set what back pressure should be used for executing requests.
	 * @param executionBackPressure the back pressure implementation
	 * @return the builder
	 */
	public NettyHttpClientBuilder setExecutionBackPressure(BackPressure executionBackPressure) {
		this.executionBackPressure = executionBackPressure;
		return this;
	}

	/**
	 * Set a custom root event bus
	 * @param rootEventBus the root event bus
	 * @return the builder
	 */
	public NettyHttpClientBuilder setRootEventBus(RootEventBus rootEventBus) {
		this.rootEventBus = rootEventBus;
		return this;
	}

	/**
	 * Set a custom socket channel pool. Defaults to {@link com.king.platform.net.http.netty.pool.PoolingChannelPool}
	 * If no pooling of connections is wanted, please provide {@link com.king.platform.net.http.netty.pool.NoChannelPool}
	 * @param channelPool the channel pool to use
	 * @return the builder
	 */
	public NettyHttpClientBuilder setChannelPool(ChannelPool channelPool) {
		this.channelPool = channelPool;
		return this;
	}

	/**
	 * Set the timeout time in ms for keep alive connections. Defaults to 30000 ms
	 * @param ms the time after which the connection will be closed (in ms)
	 * @return the builder
	 */
	public NettyHttpClientBuilder setKeepAliveTimeoutMs(int ms) {
		if (channelPool != null) {
			throw new IllegalStateException("Can't set keep-alive timeout when a non-default channel pool has already been set.");
		}
		this.keepAliveTimeoutMS = ms;
		return this;
	}

	/**
	 * Create a HttpClient instance with the current settings.
	 * @return the built HttpClient
	 */
	public HttpClient createHttpClient() {
		List<NettyHttpClient.ShutdownJob> shutdownJobs = new ArrayList<>();

		if (httpCallbackExecutor == null) {
			if (httpCallbackExecutorThreads == 0) {
				httpCallbackExecutorThreads = 2;
			}
			final ExecutorService executorService = Executors.newFixedThreadPool(httpCallbackExecutorThreads, newThreadFactory("HttpClient-HttpCallback"));
			httpCallbackExecutor = executorService;

			shutdownJobs.add(new NettyHttpClient.ShutdownJob() {
				@Override
				public void onShutdown() {
					executorService.shutdown();
				}
			});
		}

		if (httpExecuteExecutor == null) {
			if (httpExecuteExecutorThreads == 0) {
				httpExecuteExecutorThreads = 2;
			}
			final ExecutorService executorService = Executors.newFixedThreadPool(httpExecuteExecutorThreads, newThreadFactory("HttpClient-Executor"));
			httpExecuteExecutor = executorService;

			shutdownJobs.add(new NettyHttpClient.ShutdownJob() {
				@Override
				public void onShutdown() {
					executorService.shutdown();
				}
			});
		}

		if (nioThreadFactory == null) {
			this.nioThreadFactory = newThreadFactory("HttpClient-nio-event-loop");
		}

		if (cleanupTimer == null) {
			cleanupTimer = new HashedWheelTimer();

			shutdownJobs.add(new NettyHttpClient.ShutdownJob() {
				@Override
				public void onShutdown() {
					cleanupTimer.stop();
				}
			});
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
			channelPool = new PoolingChannelPool(cleanupTimer, timeProvider, keepAliveTimeoutMS, metricCallback);
		}

		if (executionBackPressure == null) {
			executionBackPressure = new NoBackPressure();
		}

		NettyHttpClient nettyHttpClient = new NettyHttpClient(nioThreads, nioThreadFactory, httpCallbackExecutor, httpExecuteExecutor, cleanupTimer, timeProvider, executionBackPressure,
			rootEventBus, channelPool);

		for (NettyHttpClient.ShutdownJob shutdownJob : shutdownJobs) {
			nettyHttpClient.addShutdownJob(shutdownJob);
		}

		return nettyHttpClient;
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
