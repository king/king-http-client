// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.backpressure;

import com.king.platform.net.http.netty.ServerInfo;

public interface BackPressure {
	/**
	 * This will be called for each created request
	 * If this returns false, an ERROR will be triggered and releaseSlot will be called directly for the same serverInfo.
	 * @param serverInfo the server the request is created against
	 * @return true if the connection should be allowed
	 */
	boolean acquireSlot(ServerInfo serverInfo);

	/**
	 * This will be called for COMPLETED and ERROR events on each request
	 * @param serverInfo the server the request was created against
	 */
	void releaseSlot(ServerInfo serverInfo);

}
