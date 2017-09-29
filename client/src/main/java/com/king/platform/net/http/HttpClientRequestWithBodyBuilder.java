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
import java.util.function.Supplier;

public interface HttpClientRequestWithBodyBuilder extends HttpClientRequestHeaderBuilder<HttpClientRequestWithBodyBuilder> {

	/**
	 * Set what content type the sent body has
	 * @param contentType the content type
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder contentType(String contentType);

	/**
	 * Set what byte[] to post/put to the server
	 * @param content the content to send in
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder content(byte[] content);

	/**
	 * Set what File to send to the server. The file will be streamed with the least overhead (zero copy for non-ssl connection).
	 * @param file the file to send in
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder content(File file);

	/**
	 * Set a custom httpBody to post/put to the server.
	 * @param httpBody the custom http body implementation
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder content(HttpBody httpBody);

	/**
	 * Set what InputStream to stream to the server.
	 * @param inputStream the InputStream to stream
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder content(InputStream inputStream);

	/**
	 * Set the encoding of the body
	 * @param charset the charset encoding type
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder bodyCharset(Charset charset);

	/**
	 * Add a parameter to a form urlencoded body
	 * @param name the parameter name
	 * @param value the parameter value
	 * @return the body
	 */
	HttpClientRequestWithBodyBuilder addFormParameter(String name, String value);

	/**
	 * Add multiple parameters to a form urlencoded body
	 * @param parameters the parameter map
	 * @return the builder
	 */
	HttpClientRequestWithBodyBuilder addFormParameters(Map<String, String> parameters);

	/**
	 * Build the request. The response is consumed as String.
	 * @return the built request
	 */
	BuiltClientRequestWithBody<String> build();

	/**
	 * Build the request. The response is consumed through the responseBodyConsumer
	 * @param responseBodyConsumer the consumer for this request
	 * @param <T> the type returned by the completed responseBodyConsumer
	 * @return the build request
	 */
	<T> BuiltClientRequestWithBody<T> build(Supplier<ResponseBodyConsumer<T>> responseBodyConsumer);

}
