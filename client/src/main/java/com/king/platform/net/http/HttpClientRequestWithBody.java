// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import com.king.platform.net.http.netty.request.HttpBody;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public interface HttpClientRequestWithBody extends HttpClientRequest {

	HttpClientRequestWithBody withHeader(String name, String value);

	HttpClientRequestWithBody keepAlive(boolean keepAlive);

	HttpClientRequestWithBody acceptCompressedResponse(boolean acceptCompressedResponse);

	HttpClientRequestWithBody withQueryParameter(String name, String value);

	HttpClientRequestWithBody idleTimeoutMillis(int readTimeoutMillis);

	HttpClientRequestWithBody totalRequestTimeoutMillis(int requestTimeoutMillis);

	HttpClientRequestWithBody followRedirects(boolean followRedirects);

	HttpClientRequestWithBody contentType(String contentType);

	HttpClientRequestWithBody content(byte[] content);

	HttpClientRequestWithBody content(File file);

	HttpClientRequestWithBody content(HttpBody httpBody);

	HttpClientRequestWithBody content(InputStream inputStream);

	HttpClientRequestWithBody bodyCharset(Charset charset);

	HttpClientRequestWithBody addFormParameter(String name, String value);

	HttpClientRequestWithBody addFormParameters(Map<String, String> parameters);


}
