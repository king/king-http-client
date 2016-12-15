// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;

import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.eventbus.DefaultEventBus;
import com.king.platform.net.http.netty.eventbus.Event;
import org.junit.Before;
import org.junit.Test;

import static se.mockachino.Mockachino.*;


public class MetricCollectorTest {
	private MetricCallback metricCallback;
	private DefaultEventBus rootEventBus;
	private ServerInfo serverInfo;
	private HttpRequestContext httpRequestContext;
	private TimeStampRecorder recordedTimeStamps;
	private String host;

	@Before
	public void setUp() throws Exception {
		MetricCollector metricCollector = new MetricCollector();
		rootEventBus = new DefaultEventBus();
		metricCallback = mock(MetricCallback.class);
		metricCollector.wireMetricCallbackOnEventBus(metricCallback, rootEventBus);
		serverInfo = new ServerInfo("http", "localhost", 8081);

		httpRequestContext = mock(HttpRequestContext.class);
		when(httpRequestContext.getServerInfo()).thenReturn(serverInfo);
		recordedTimeStamps = mock(TimeStampRecorder.class);
		when(httpRequestContext.getTimeRecorder()).thenReturn(recordedTimeStamps);
		host = serverInfo.getHost();
	}

	@Test
	public void createdConnection() throws Exception {
		rootEventBus.triggerEvent(Event.CREATED_CONNECTION, serverInfo);
		verifyOnce().on(metricCallback).onCreatedConnectionTo(host);
	}

	@Test
	public void reusedConnection() throws Exception {
		rootEventBus.triggerEvent(Event.REUSED_CONNECTION, serverInfo);
		verifyOnce().on(metricCallback).onReusedConnectionTo(host);
	}

	@Test
	public void closedConnection() throws Exception {
		rootEventBus.triggerEvent(Event.CLOSED_CONNECTION, serverInfo);
		verifyOnce().on(metricCallback).onClosedConnectionTo(host);
	}

	@Test
	public void error() throws Exception {
		rootEventBus.triggerEvent(Event.ERROR, httpRequestContext, new Throwable());
		verifyOnce().on(metricCallback).onError(host, recordedTimeStamps);
	}

	@Test
	public void completed() throws Exception {
		rootEventBus.triggerEvent(Event.COMPLETED, httpRequestContext);
		verifyOnce().on(metricCallback).onCompletedRequest(host, recordedTimeStamps);

	}
}
