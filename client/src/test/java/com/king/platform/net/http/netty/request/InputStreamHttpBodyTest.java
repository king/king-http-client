// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.request;

import io.netty.channel.*;
import io.netty.handler.stream.ChunkedStream;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class InputStreamHttpBodyTest {
	private InputStreamHttpBody inputStreamHttpBody;
	private InputStream inputStream;

	@BeforeEach
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

		when(channel.write(any(), any())).thenReturn(channelFuture);
		when(channelFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new Answer<ChannelFuture>() {
			@Override
			public ChannelFuture answer(InvocationOnMock invocation) throws Throwable {
				ChannelFutureListener channelFutureListener = (ChannelFutureListener) invocation.getArgument(0);
				ChannelFuture future = mock(ChannelFuture.class);
				channelFutureListener.operationComplete(future);
				return future;
			}
		});

		inputStreamHttpBody.writeContent(ctx, false);

		verify(inputStream).close();
	}
}
