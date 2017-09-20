// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


public interface BuiltClientRequestWithBody<T> extends BaseBuiltClientRequest<T, BuiltClientRequest<T>> {
	BuiltClientRequestWithBody<T> withUploadCallback(UploadCallback uploadCallback);
}
