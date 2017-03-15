// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;

import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.util.TimeProviderForTesting;
import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import se.mockachino.CallHandler;
import se.mockachino.MethodCall;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static se.mockachino.Mockachino.*;


public class ServerPoolTest {
	private ServerPool serverPool;
	private TimeProviderForTesting timeProvider;

	@Before
	public void setUp() throws Exception {
		timeProvider = new TimeProviderForTesting();
		serverPool = new ServerPool(ServerInfo.buildFromUri("http://localhost/"), 100, TimeUnit.SECONDS, timeProvider, mock(MetricCallback.class));

	}

	@Test
	public void offerOfValidChannelShouldStoreIt() throws Exception {
		serverPool.offer(createStateFullChannel());
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());
	}

	@Test
	public void offerOfClosedChannelShouldNotStoreIt() throws Exception {
		Channel channel = createStateFullChannel();
		channel.close();
		serverPool.offer(channel);
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
	}

	@Test
	public void discardingAChannelShouldRemoveAndCloseIt() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel);
		serverPool.discard(validChannel);

		assertEquals(0, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize()); //the channel should still be in the pool since its not removed from the queue
		verifyOnce().on(validChannel)
			.close();
	}

	@Test
	public void cleanShouldRemoveDiscardedConnections() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel);
		serverPool.discard(validChannel);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getPoolSize()); //the pool is now empty since it was explicitly cleaned
	}

	@Test
	public void cleanShouldRemoveToOldChannels() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel);
		timeProvider.forwardSeconds(120);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verifyOnce().on(validChannel)
			.close();

	}

	@Test
	public void cleanShouldNotRemoveWorkingChannels() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel);
		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());
		verifyNever().on(channel)
			.close();

	}

	@Test
	public void cleanShouldNotRemovePolledChannelIfTheyAreNotOverMaxTTL() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel);
		serverPool.poll();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		verifyNever().on(channel).close();
	}

	@Test
	public void cleanShouldNotRemovePolledChannelIfTheyAreOverMaxTTLButTheyArePolled() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel);
		serverPool.poll();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		timeProvider.forwardSeconds(101);
		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verifyNever().on(channel).close();
	}

	@Test
	public void cleanShouldRemovePolledChannelIfTheyAreOverMaxTTL() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel);
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());

		timeProvider.forwardSeconds(101);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verifyOnce().on(channel).close();
	}

	@Test
	public void pollShouldReturnOpenChannel() throws Exception {
		Channel stateFullChannel = createStateFullChannel();
		serverPool.offer(stateFullChannel);
		Channel channel = serverPool.poll();
		assertSame(stateFullChannel, channel);
	}

	@Test
	public void pollShouldReturnNotReturnClosedChannel() throws Exception {
		Channel stateFullChannel = createStateFullChannel();
		serverPool.offer(stateFullChannel);
		stateFullChannel.close();
		Channel channel = serverPool.poll();
		assertNull(channel);
	}


	private Channel createStateFullChannel() {
		final AtomicBoolean state = new AtomicBoolean(true);

		Channel channel = mock(Channel.class);

		CallHandler answer = new CallHandler() {
			@Override
			public Object invoke(Object obj, MethodCall call) throws Throwable {
				return state.get();
			}
		};

		when(channel.isActive()).thenAnswer(answer);
		when(channel.isOpen()).thenAnswer(answer);

		when(channel.close()).thenAnswer(new CallHandler() {
			@Override
			public Object invoke(Object obj, MethodCall call) throws Throwable {
				state.set(false);
				return null;
			}
		});

		return channel;
	}
}
