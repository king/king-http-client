package com.king.platform.net.http;

import java.time.Duration;

public interface HttpClientWebSocketRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientWebSocketRequestBuilder> {

	HttpClientWebSocketRequestBuilder subProtocols(String subProtocols);

	HttpClientWebSocketRequestBuilder withPingEvery(Duration duration);

	HttpClientWebSocketRequestBuilder withAutoPong(boolean autoPong);

	HttpClientWebSocketRequestBuilder withAutoCloseFrame(boolean autoCloseFrame);

	BuiltWebSocketRequest build();

}
