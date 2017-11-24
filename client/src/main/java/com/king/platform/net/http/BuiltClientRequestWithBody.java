// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.util.function.Supplier;

public interface BuiltClientRequestWithBody<T> extends BaseBuiltClientRequest<T, BuiltClientRequest<T>> {
	/**
	 * Use a specific upload callback for all execution of this request.
	 * UploadCallbacks are executed on the HttpCallbackExecutor
	 *
	 * @param uploadCallback the upload callback
	 * @return this builder
	 */
	BuiltClientRequestWithBody<T> withUploadCallback(UploadCallback uploadCallback);

	/**
	 * Each execution will use a UploadCallback supplied from the supplier.
	 *
	 * @param uploadCallback the upload callback
	 * @return this builder
	 */
	BuiltClientRequestWithBody<T> withUploadCallback(Supplier<UploadCallback> uploadCallback);
}
