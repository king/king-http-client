package com.king.platform.net.http;


public interface HttpClientSseRequestBuilder extends HttpClientRequestHeaderBuilder<HttpClientSseRequestBuilder>  {
	BuiltSseClientRequest build();

}
