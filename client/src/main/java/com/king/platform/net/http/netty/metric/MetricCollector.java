// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;


import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.eventbus.*;

public class MetricCollector {


	public void wireMetricCallbackOnEventBus(final MetricCallback metricCallback, final RootEventBus rootEventBus) {

		rootEventBus.subscribePermanently(Event.CREATED_CONNECTION, new EventBusCallback1<ServerInfo>() {
			@Override
			public void onEvent(Event1<ServerInfo> event, ServerInfo payload) {
				metricCallback.onCreatedConnectionTo(payload.getHost());
			}
		});


		rootEventBus.subscribePermanently(Event.REUSED_CONNECTION, new EventBusCallback1<ServerInfo>() {
			@Override
			public void onEvent(Event1<ServerInfo> event, ServerInfo payload) {
				metricCallback.onReusedConnectionTo(payload.getHost());
			}
		});


		rootEventBus.subscribePermanently(Event.CLOSED_CONNECTION, new EventBusCallback1<ServerInfo>() {
			@Override
			public void onEvent(Event1<ServerInfo> event, ServerInfo payload) {
				metricCallback.onClosedConnectionTo(payload.getHost());
			}
		});


		rootEventBus.subscribePermanently(Event.ERROR, new EventBusCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onEvent(Event2<HttpRequestContext, Throwable> event, HttpRequestContext payload1, Throwable payload2) {
				metricCallback.onError(payload1.getServerInfo().getHost(), payload1.getTimeRecorder());
			}
		});


		rootEventBus.subscribePermanently(Event.COMPLETED, new EventBusCallback1<HttpRequestContext>() {
			@Override
			public void onEvent(Event1<HttpRequestContext> event, HttpRequestContext payload) {
				metricCallback.onCompletedRequest(payload.getServerInfo().getHost(), payload.getTimeRecorder());
			}
		});
	}

}
