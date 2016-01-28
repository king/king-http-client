package com.king.platform.net.http;


public interface HttpClientSSERequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientSSERequestBuilder>  {
	BuiltSSEClientRequest build();

}
