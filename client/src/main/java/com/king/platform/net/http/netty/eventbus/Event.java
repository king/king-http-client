// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.eventbus;

import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ServerInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface Event {
	Event2<HttpRequestContext, Throwable> ERROR = new Event2<>("Error");
	Event1<HttpRequestContext> COMPLETED = new Event1<>("Completed");
	Event1<Void> TOUCH = new Event1<>("Touch");
	Event1<Void> CLOSE = new Event1<>("Close");

	Event1<HttpRequestContext> EXECUTE_REQUEST = new Event1<>("ExecuteRequest");
	Event1<ChannelHandlerContext> WRITE_BODY = new Event1<>("WriteDelayedBody");
	Event2<ServerInfo, HttpHeaders> POPULATE_CONNECTION_SPECIFIC_HEADERS = new Event2<>("PopulateConnectionSpecificHeaders");

	Event1<ServerInfo> CREATED_CONNECTION = new Event1<>("CreatedConnection");
	Event1<ServerInfo> REUSED_CONNECTION = new Event1<>("ReusedConnection");
	Event1<ServerInfo> POOLED_CONNECTION = new Event1<>("PooledConnection");
	Event1<ServerInfo> CLOSED_CONNECTION = new Event1<>("CloseConnection");

	Event1<Void> onConnecting = new Event1<>("onConnecting");
	Event1<Void> onConnected = new Event1<>("onConnected");
	Event1<Void> onWroteHeaders = new Event1<>("onWroteHeaders");
	Event1<Long> onWroteContentStarted = new Event1<>("onWroteContentStarted");
	Event2<Long, Long> onWroteContentProgressed = new Event2<>("onWroteContentProgressed");
	Event1<Void> onWroteContentCompleted = new Event1<>("onWroteContentCompleted");
	Event1<HttpResponseStatus> onReceivedStatus = new Event1<>("onReceivedStatus");
	Event1<HttpHeaders> onReceivedHeaders = new Event1<>("onReceivedHeaders");
	Event2<Integer, ByteBuf> onReceivedContentPart = new Event2<>("onReceivedContentPart");
	Event2<HttpResponseStatus, HttpHeaders> onReceivedCompleted = new Event2<>("onReceivedCompleted");

	Event1<HttpResponse> onHttpResponseDone = new Event1<>("onHttpResponseDone");


	Event1<ChannelPipeline> WS_UPGRADE_PIPELINE = new Event1<>("WS_UPGRADE_PIPELINE");
	Event2<Channel, HttpHeaders> onWsOpen = new Event2<>("onWsOpen");
	Event1<WebSocketFrame> onWsFrame = new Event1<>("onWsFrame");


	String getName();
}
