package com.king.platform.net.http;

public interface HttpClientWebSocketRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientWebSocketRequestBuilder> {

	HttpClientWebSocketRequestBuilder subProtocols(String subProtocols);


	BuiltWebSocketRequest build();

}
