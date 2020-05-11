// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;


import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RootEventBus;

public class MetricCollector {


	public void wireMetricCallbackOnEventBus(final MetricCallback metricCallback, final RootEventBus rootEventBus) {

		rootEventBus.subscribePermanently(Event.CREATED_CONNECTION, (payload) -> metricCallback.onCreatedConnectionTo(payload.getHost()));


		rootEventBus.subscribePermanently(Event.REUSED_CONNECTION, (payload) -> metricCallback.onReusedConnectionTo(payload.getHost()));


		rootEventBus.subscribePermanently(Event.CLOSED_CONNECTION, (payload) -> metricCallback.onClosedConnectionTo(payload.getHost()));


		rootEventBus.subscribePermanently(Event.ERROR, (payload1, payload2) -> metricCallback.onError(payload1.getServerInfo().getHost(), payload1.getTimeRecorder()));


		rootEventBus.subscribePermanently(Event.onInternalCompletion, (payload) -> metricCallback.onCompletedRequest(payload.getServerInfo().getHost(), payload.getTimeRecorder()));
	}

}
