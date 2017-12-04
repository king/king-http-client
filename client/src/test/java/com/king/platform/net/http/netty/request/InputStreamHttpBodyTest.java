// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;

import io.netty.channel.*;
import io.netty.handler.stream.ChunkedStream;
import org.junit.Before;
import org.junit.Test;
import se.mockachino.CallHandler;
import se.mockachino.MethodCall;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static se.mockachino.Mockachino.*;
import static se.mockachino.matchers.Matchers.any;


public class InputStreamHttpBodyTest {
	private InputStreamHttpBody inputStreamHttpBody;
	private InputStream inputStream;

	@Before
	public void setUp() throws Exception {
		inputStream = mock(InputStream.class);
		inputStreamHttpBody = new InputStreamHttpBody(inputStream, "test/content", StandardCharsets.ISO_8859_1);

	}

	@Test
	public void operationCompletedShouldCloseInputStream() throws Exception {
		ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
		Channel channel = mock(Channel.class);
		when(ctx.channel()).thenReturn(channel);

		ChannelFuture channelFuture = mock(ChannelFuture.class);
		when(channel.write(any(ChunkedStream.class), any(ChannelPromise.class))).thenReturn(channelFuture);

		when(channelFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new CallHandler() {
			@Override
			public Object invoke(Object obj, MethodCall call) throws Throwable {
				ChannelFutureListener channelFutureListener = (ChannelFutureListener) call.getArguments()[0];
				channelFutureListener.operationComplete(mock(ChannelFuture.class));
				return null;
			}
		});

		inputStreamHttpBody.writeContent(ctx, false);

		verifyOnce().on(inputStream).close();
	}
}
