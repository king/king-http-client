// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.response;


import com.king.platform.net.http.KingHttpException;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.ServerInfo;
import com.king.platform.net.http.netty.eventbus.Event;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;

import java.net.URISyntaxException;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpRedirector {

	private final Logger logger = getLogger(getClass());

	private final int[] REDIRECTABLE_STATUS_CODES = {301, 302, 303, 307, 308};


	public boolean isRedirectResponse(HttpResponseStatus httpResponseStatus) {
		int statusCode = httpResponseStatus.code();
		boolean redirect = Arrays.binarySearch(REDIRECTABLE_STATUS_CODES, statusCode) >= 0;
		if (redirect) {
			logger.trace("Should redirect since statusCode is {}", statusCode);
		}
		return redirect;
	}

	public void redirectRequest(HttpRequestContext originalRequestContext, HttpHeaders responseHttpHeaders) {

		RequestEventBus requestEventBus = originalRequestContext.getRequestEventBus();

		if (originalRequestContext.getRedirectionCount() > 5) {
			requestEventBus.triggerEvent(Event.ERROR, originalRequestContext, new KingHttpException("Max redirection count has been reached!"));
			return;
		}

		try {

			String redirectLocation = responseHttpHeaders.get(HttpHeaderNames.LOCATION);

			ServerInfo redirectServerInfo = ServerInfo.buildFromUri(redirectLocation);

			HttpRequestContext redirectHttpRequestContext = originalRequestContext.createRedirectRequest(redirectServerInfo, redirectLocation);

			requestEventBus.triggerEvent(Event.COMPLETED, originalRequestContext);

			logger.trace("Redirecting request {}", redirectHttpRequestContext);

			requestEventBus.triggerEvent(Event.EXECUTE_REQUEST, redirectHttpRequestContext);

		} catch (URISyntaxException exception) {
			requestEventBus.triggerEvent(Event.ERROR, originalRequestContext, exception);
		}

	}
}
