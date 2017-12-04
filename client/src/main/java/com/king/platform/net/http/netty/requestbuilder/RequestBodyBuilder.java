// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.netty.request.HttpBody;

import java.nio.charset.Charset;

interface RequestBodyBuilder {

	HttpBody createHttpBody(String contentType, Charset characterEncoding);
}
