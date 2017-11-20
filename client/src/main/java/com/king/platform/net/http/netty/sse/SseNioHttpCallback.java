package com.king.platform.net.http.netty.sse;


import com.king.platform.net.http.KingHttpException;
import com.king.platform.net.http.NioCallback;
import com.king.platform.net.http.netty.response.HttpRedirector;
import com.king.platform.net.http.netty.util.AwaitLatch;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SseNioHttpCallback implements NioCallback {

    private final ServerEventDecoder serverEventDecoder;
    private final DelegatingAsyncSseClientCallback delegatingAsyncSseClientCallback;
    private final AtomicReference<State> state;
    private final boolean followRedirects;
    private final AtomicBoolean isRedirecting = new AtomicBoolean();
    private final AtomicReference<HttpResponseStatus> httpResponseStatus = new AtomicReference<>();
    private final AwaitLatch awaitLatch;

    public SseNioHttpCallback(ServerEventDecoder serverEventDecoder, DelegatingAsyncSseClientCallback delegatingAsyncSseClientCallback,
							  AtomicReference<State> state, boolean followRedirects, AwaitLatch awaitLatch) {
        this.serverEventDecoder = serverEventDecoder;
        this.delegatingAsyncSseClientCallback = delegatingAsyncSseClientCallback;
        this.state = state;
        this.followRedirects = followRedirects;
        this.awaitLatch = awaitLatch;
    }

    @Override
    public void onConnecting() {
        isRedirecting.set(false);
        httpResponseStatus.set(null);
	}

    @Override
    public void onConnected() {
    }

    @Override
    public void onWroteHeaders() {
    }

    @Override
    public void onWroteContentProgressed(long progress, long total) {

    }

    @Override
    public void onWroteContentCompleted() {

    }

    @Override
    public void onReceivedStatus(final HttpResponseStatus httpResponseStatus) {
        if (HttpRedirector.isRedirectResponse(httpResponseStatus) && followRedirects) {
            isRedirecting.set(true);
        } else {
            this.httpResponseStatus.set(httpResponseStatus);
        }

    }

    @Override
    public void onReceivedHeaders(HttpHeaders httpHeaders) {
        if (isRedirecting.get()) {
            return;
        }

        final int httpStatus = httpResponseStatus.get().code();


        if (httpStatus != 200) {
			delegatingAsyncSseClientCallback.onError(new KingHttpException("Invalid http status " + httpStatus + " reason was " + httpResponseStatus.get()
                .reasonPhrase()));
            return;
        }


        final String contentType = httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "null");
        if (!contentType.toLowerCase().contains("text/event-stream")) {
            delegatingAsyncSseClientCallback.onError(new KingHttpException("Invalid content-type:" + contentType));
            return;
        }

        state.set(State.CONNECTED);
        delegatingAsyncSseClientCallback.onConnect();
    }

    @Override
    public void onReceivedContentPart(int len, ByteBuf buffer) {
        if (state.get() == State.CONNECTED) {
        	try {
				serverEventDecoder.onReceivedContentPart(buffer);
			} catch (Exception e) {
        		onError(e);
			}
        }
    }

    @Override
    public void onReceivedCompleted(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {
		if (state.get() == State.CONNECTED) {
			delegatingAsyncSseClientCallback.onDisconnect();
		}

        state.set(State.DISCONNECTED);
        awaitLatch.closed();
    }

    @Override
    public void onError(Throwable throwable) {
		delegatingAsyncSseClientCallback.onError(throwable);
        state.set(State.DISCONNECTED);
        awaitLatch.closed();
    }
}
