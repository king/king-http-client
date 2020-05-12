// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.EnumSet;

public class JettyIntegrationServer implements IntegrationServer {
	private final int maxParallelThreads;
	private int port;
	private Server server;
	private ServletContextHandler servletContextHandler;

	public JettyIntegrationServer() {
		this(20);
	}

	public JettyIntegrationServer(int maxParallelThreads) {
		this.maxParallelThreads = maxParallelThreads;
	}


	@Override
	public void start() throws Exception {
		start(false);

	}

	@Override
	public void startHttps() throws Exception {
		start(true);
	}

	private void start(boolean useHttps) throws Exception {
		this.port = findFreePort();

		server = new Server(new QueuedThreadPool(maxParallelThreads, 10, 5000, new BlockingArrayQueue<Runnable>(200, 20, 800)));

		if (useHttps) {
			URL keyStoreUrl = getClass().getResource("/keystore.jks");
			String keyStoreFile = new File(keyStoreUrl.toURI()).getAbsolutePath();
			SslContextFactory sslContextFactory = new SslContextFactory.Client();
			sslContextFactory.setKeyStorePath(keyStoreFile);
			//SslContextFactory sslContextFactory = new SslContextFactory(keyStoreFile);
			sslContextFactory.setKeyStorePassword("changeme");


			HttpConfiguration httpsConfig = new HttpConfiguration();
			httpsConfig.setSecureScheme("https");
			httpsConfig.setSecurePort(port);
			httpsConfig.addCustomizer(new SecureRequestCustomizer());

			ServerConnector connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
			connector.setPort(port);

			server.addConnector(connector);

		} else {
			ServerConnector serverConnector = new ServerConnector(server);
			serverConnector.setPort(port);
			serverConnector.setAcceptQueueSize(400);
			server.addConnector(serverConnector);
		}

		servletContextHandler = new ServletContextHandler();
		server.setHandler(servletContextHandler);


		server.start();
	}

	@Override
	public void addServlet(HttpServlet servlet, String path) {
		servletContextHandler.addServlet(new ServletHolder(servlet), path);
	}

	@Override
	public void addFilter(FilterHolder filterHolder, String path) {
		servletContextHandler.addFilter(filterHolder, path, EnumSet.allOf(DispatcherType.class));
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void shutdown() throws Exception {
		server.stop();
	}

	public static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
