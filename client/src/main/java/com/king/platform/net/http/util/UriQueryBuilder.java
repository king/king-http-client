// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.util;


import com.king.platform.net.http.netty.util.ParameterEncoder;

import java.net.URI;
import java.net.URISyntaxException;

public class UriQueryBuilder {
	private final ParameterEncoder parameterEncoder = new ParameterEncoder();
	private final StringBuilder completeUrl;

	public UriQueryBuilder(String uri) {
		completeUrl = new StringBuilder(uri);

		if (uri.endsWith("?") || uri.endsWith("&")) {
			return;
		}

		if (uri.contains("?")) {
			completeUrl.append("&");
		} else {
			completeUrl.append("?");
		}
	}

	public UriQueryBuilder addParameter(CharSequence name, CharSequence value) {
		parameterEncoder.addParameter(completeUrl, name, value);
		return this;
	}

	public String build() {
		return completeUrl.substring(0, completeUrl.length() - 1);
	}

	public URI buildUri() throws URISyntaxException {
		return new URI(build());
	}

}
