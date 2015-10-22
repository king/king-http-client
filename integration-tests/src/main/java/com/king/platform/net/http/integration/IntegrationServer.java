// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import org.eclipse.jetty.servlet.FilterHolder;

import javax.servlet.http.HttpServlet;

public interface IntegrationServer {
	void start() throws Exception;

	void startHttps() throws Exception;

	int getPort();

	void shutdown() throws Exception;

	void addServlet(HttpServlet servlet, String path);

	void addFilter(FilterHolder filterHolder, String path);
}
