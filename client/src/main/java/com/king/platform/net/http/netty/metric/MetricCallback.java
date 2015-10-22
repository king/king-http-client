// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.metric;


public interface MetricCallback {
	void onClosedConnectionTo(String host);

	void onCreatedConnectionTo(String host);

	void onReusedConnectionTo(String host);

	void onError(String host, RecordedTimeStamps timeStampRecorder);

	void onCompletedRequest(String host, RecordedTimeStamps recordedTimeStamps);

	void onCreatedServerPool(String host);

	void onRemovedServerPool(String host);

	void onServerPoolClosedConnection(String host, int poolSize);

	void onServerPoolAddedConnection(String host, int poolSize);
}
