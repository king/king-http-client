package com.king.platform.net.http;

public interface HttpClientWebSocketRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientWebSocketRequestBuilder> {

	BuiltWebSocketRequest build();

}
