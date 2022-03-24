// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.pool;

import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.metric.MetricCallback;
import com.king.platform.net.http.netty.util.TimeProviderForTesting;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ServerPoolTest {
	private ServerPool serverPool;
	private TimeProviderForTesting timeProvider;
	private int keepAliveTimeoutMillis = 100*1000;
	@BeforeEach
	public void setUp() throws Exception {
		timeProvider = new TimeProviderForTesting();
		serverPool = new ServerPool(ServerInfo.buildFromUri("http://localhost/"),  timeProvider, mock(MetricCallback.class));

	}

	@Test
	public void offerOfValidChannelShouldStoreIt() throws Exception {
		serverPool.offer(createStateFullChannel(), keepAliveTimeoutMillis);
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());
	}

	@Test
	public void offerOfClosedChannelShouldNotStoreIt() throws Exception {
		Channel channel = createStateFullChannel();
		channel.close();
		serverPool.offer(channel, keepAliveTimeoutMillis);
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
	}

	@Test
	public void discardingAChannelShouldRemoveAndCloseIt() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel, keepAliveTimeoutMillis);
		serverPool.discard(validChannel);

		assertEquals(0, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize()); //the channel should still be in the pool since its not removed from the queue
		verify(validChannel).close();
	}

	@Test
	public void cleanShouldRemoveDiscardedConnections() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel, keepAliveTimeoutMillis);
		serverPool.discard(validChannel);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getPoolSize()); //the pool is now empty since it was explicitly cleaned
	}

	@Test
	public void cleanShouldRemoveToOldChannels() throws Exception {
		Channel validChannel = createStateFullChannel();
		serverPool.offer(validChannel, keepAliveTimeoutMillis);
		timeProvider.forwardSeconds(120);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verify(validChannel).close();

	}

	@Test
	public void cleanShouldNotRemoveWorkingChannels() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel, keepAliveTimeoutMillis);
		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());
		verify(channel, times(0)).close();

	}

	@Test
	public void cleanShouldNotRemovePolledChannelIfTheyAreNotOverMaxTTL() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel, keepAliveTimeoutMillis);
		serverPool.poll();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		verify(channel, times(0)).close();
	}

	@Test
	public void cleanShouldNotRemovePolledChannelIfTheyAreOverMaxTTLButTheyArePolled() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel, keepAliveTimeoutMillis);
		serverPool.poll();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());

		timeProvider.forwardSeconds(101);
		serverPool.cleanExpiredConnections();
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verify(channel, times(0)).close();
	}

	@Test
	public void cleanShouldRemovePolledChannelIfTheyAreOverMaxTTL() throws Exception {
		Channel channel = createStateFullChannel();
		serverPool.offer(channel, keepAliveTimeoutMillis);
		assertEquals(1, serverPool.getChannelSize());
		assertEquals(1, serverPool.getPoolSize());

		timeProvider.forwardSeconds(101);
		serverPool.cleanExpiredConnections();
		assertEquals(0, serverPool.getChannelSize());
		assertEquals(0, serverPool.getPoolSize());
		verify(channel).close();
	}

	@Test
	public void pollShouldReturnOpenChannel() throws Exception {
		Channel stateFullChannel = createStateFullChannel();
		serverPool.offer(stateFullChannel, keepAliveTimeoutMillis);
		Channel channel = serverPool.poll();
		assertSame(stateFullChannel, channel);
	}

	@Test
	public void pollShouldReturnNotReturnClosedChannel() throws Exception {
		Channel stateFullChannel = createStateFullChannel();
		serverPool.offer(stateFullChannel, keepAliveTimeoutMillis);
		stateFullChannel.close();
		Channel channel = serverPool.poll();
		assertNull(channel);
	}


	private Channel createStateFullChannel() {
		final AtomicBoolean state = new AtomicBoolean(true);

		Channel channel = mock(Channel.class);


		Answer<Boolean> answer = new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return state.get();
			}
		};
		when(channel.isActive()).thenAnswer(answer);
		when(channel.isOpen()).thenAnswer(answer);

		when(channel.close()).thenAnswer(new Answer<ChannelFuture>() {
			@Override
			public ChannelFuture answer(InvocationOnMock invocation) throws Throwable {
				state.set(false);
				return null;
			}
		});

		return channel;
	}
}
