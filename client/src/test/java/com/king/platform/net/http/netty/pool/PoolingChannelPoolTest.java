// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;

import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.util.TimeProviderForTesting;
import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static se.mockachino.Mockachino.mock;
import static se.mockachino.Mockachino.when;

public class PoolingChannelPoolTest {

	PoolingChannelPool poolingChannelPool;
	private ServerInfo serverInfo;
	private TimeProviderForTesting timeProvider;
	private TestTimer timer;
	private Channel activeChannel;
	private Channel inactiveChannel;
	private Channel closedChannel;

	@Before
	public void setUp() throws Exception {
		activeChannel = getActiveChannelMock();

		inactiveChannel = getActiveChannelMock();
		when(inactiveChannel.isActive()).thenReturn(false);

		closedChannel = getActiveChannelMock();
		when(closedChannel.isOpen()).thenReturn(false);


		timeProvider = new TimeProviderForTesting();
		timer = new TestTimer();

		poolingChannelPool = new PoolingChannelPool(timer, timeProvider, 15, mock(MetricCallback.class));

		serverInfo = ServerInfo.buildFromUri("http://somehost:8081/foo/bar");

	}

	private Channel getActiveChannelMock() {
		Channel channel = mock(Channel.class);
		when(channel.isActive()).thenReturn(true);
		when(channel.isOpen()).thenReturn(true);
		return channel;
	}

	@Test
	public void offerOfAnChannel() throws Exception {

		poolingChannelPool.offer(serverInfo, activeChannel);

		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void offerOfTwoChannels() throws Exception {
		poolingChannelPool.offer(ServerInfo.buildFromUri("http://somehost:8081/foo1/bar1"), getActiveChannelMock());
		poolingChannelPool.offer(ServerInfo.buildFromUri("http://somehost:8081/foo2/bar2"), getActiveChannelMock());

		assertEquals(2, poolingChannelPool.getPoolSize(ServerInfo.buildFromUri("http://somehost:8081/foo3/bar3")));
	}


	@Test
	public void offerOfAnInactiveChannelShouldNotPool() throws Exception {

		poolingChannelPool.offer(serverInfo, inactiveChannel);

		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void offerOfAnClosedChannelShouldNotPool() throws Exception {
		poolingChannelPool.offer(serverInfo, closedChannel);
		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void getOfAnEmptyChannel() throws Exception {
		Channel channel = poolingChannelPool.get(serverInfo);
		assertNull(channel);
	}

	@Test
	public void getOfAnOfferedChannel() throws Exception {
		poolingChannelPool.offer(serverInfo, activeChannel);

		Channel channel = poolingChannelPool.get(serverInfo);
		assertSame(activeChannel, channel);

		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void getOfAnOfferedChannelInRightOrder() throws Exception {
		Channel channel1 = getActiveChannelMock();
		Channel channel2 = getActiveChannelMock();

		poolingChannelPool.offer(serverInfo, channel1);
		poolingChannelPool.offer(serverInfo, channel2);

		Channel fetchedChannel1 = poolingChannelPool.get(serverInfo);
		Channel fetchedChannel2 = poolingChannelPool.get(serverInfo);
		assertSame(channel1, fetchedChannel1);
		assertSame(fetchedChannel2, channel2);

		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}


	@Test
	public void getOfAnInactiveChannel() throws Exception {
		poolingChannelPool.offer(serverInfo, activeChannel);
		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));

		when(activeChannel.isActive()).thenReturn(false);
		Channel channel = poolingChannelPool.get(serverInfo);
		assertNull(channel);

		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void getOfAnClosedChannel() throws Exception {
		poolingChannelPool.offer(serverInfo, activeChannel);
		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));

		when(activeChannel.isOpen()).thenReturn(false);
		Channel channel = poolingChannelPool.get(serverInfo);
		assertNull(channel);

		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}


	@Test
	public void getOfAnExpiredChannel() throws Exception {
		poolingChannelPool.offer(serverInfo, activeChannel);
		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));

		timeProvider.forwardSeconds(14);

		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));


		timeProvider.forwardSeconds(2);

		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));
		Channel channel = poolingChannelPool.get(serverInfo);
		assertNull(channel);
		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));
	}

	@Test
	public void timerShouldRemoveExpireChannels() throws Exception {
		poolingChannelPool.offer(serverInfo, getActiveChannelMock());
		timeProvider.forwardSeconds(10);
		timer.invoke();
		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));

		poolingChannelPool.offer(serverInfo, getActiveChannelMock());
		assertEquals(2, poolingChannelPool.getPoolSize(serverInfo));

		timeProvider.forwardSeconds(5);
		timer.invoke();
		assertEquals(1, poolingChannelPool.getPoolSize(serverInfo));

		timeProvider.forwardSeconds(10);
		timer.invoke();
		assertEquals(0, poolingChannelPool.getPoolSize(serverInfo));

	}

	@Test
	public void discard() throws Exception {


	}

	private static class TestTimer implements Timer {
		private final List<TimerTask> tasks = new ArrayList<>();

		@Override
		public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
			tasks.add(task);
			return mock(Timeout.class);
		}

		@Override
		public Set<Timeout> stop() {
			return Collections.emptySet();
		}

		public void invoke() throws Exception {

			TimerTask timerTask = tasks.get(0);
			Timeout timeout = mock(Timeout.class);
			when(timeout.task()).thenReturn(timerTask);
			timerTask.run(timeout);
		}
	}
}
