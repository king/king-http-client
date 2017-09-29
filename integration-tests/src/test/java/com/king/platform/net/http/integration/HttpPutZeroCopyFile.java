// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ByteArrayResponseBodyConsumer;
import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import io.netty.util.ResourceLeakDetector;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class HttpPutZeroCopyFile {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;
	private TemporaryFile temporaryFile;

	private static String hexStringFromBytes(byte[] b) {
		return String.format("%0" + b.length * 2 + "x", new BigInteger(1, b));
	}

	@Before
	public void setUp() throws Exception {

		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);


		temporaryFile = new TemporaryFile(folder);

		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory()
			.setOption(ConfKeys.TOTAL_REQUEST_TIMEOUT_MILLIS, 0)
			.setOption(ConfKeys.IDLE_TIMEOUT_MILLIS, 1000)
			.create();


		httpClient.start();

	}

	@Test
	public void put1kFile() throws Exception {
		temporaryFile.generateContent(1);

		integrationServer.addServlet(new MD5CalculatingHttpServlet(), "/putFile");

		BlockingBinaryHttpCallback httpCallback = new BlockingBinaryHttpCallback();
		httpClient.createPut("http://localhost:" + port + "/putFile").content(temporaryFile.getFile()).build(ByteArrayResponseBodyConsumer::new).withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();

		assertEquals(200, httpCallback.getStatusCode());

		byte[] serverMd5 = httpCallback.getBody();

		assertEquals(hexStringFromBytes(temporaryFile.getFileMd5()), hexStringFromBytes(serverMd5));

	}

	@Test
	public void put32MBFile() throws Exception {
		temporaryFile.generateContent(32 * 1024);

		integrationServer.addServlet(new MD5CalculatingHttpServlet(), "/putFile");

		BlockingBinaryHttpCallback httpCallback = new BlockingBinaryHttpCallback();

		httpClient.createPut("http://localhost:" + port + "/putFile").content(temporaryFile.getFile()).build(ByteArrayResponseBodyConsumer::new).withHttpCallback(httpCallback).execute();

		httpCallback.waitForCompletion();

		assertEquals(200, httpCallback.getStatusCode());

		byte[] serverMd5 = httpCallback.getBody();

		assertEquals(hexStringFromBytes(temporaryFile.getFileMd5()), hexStringFromBytes(serverMd5));

	}

	@Test
	public void put64MBFile() throws Exception {


		temporaryFile.generateContent(64 * 1024);

		integrationServer.addServlet(new MD5CalculatingHttpServlet(), "/putFile");

		BlockingBinaryHttpCallback httpCallback = new BlockingBinaryHttpCallback();
		httpClient.createPut("http://localhost:" + port + "/putFile").contentType("multipart/form-data").
			content(temporaryFile.getFile()).build(ByteArrayResponseBodyConsumer::new).withHttpCallback(httpCallback).execute();
		httpCallback.waitForCompletion();


		assertEquals(200, httpCallback.getStatusCode());

		byte[] serverMd5 = httpCallback.getBody();

		assertEquals(hexStringFromBytes(temporaryFile.getFileMd5()), hexStringFromBytes(serverMd5));

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


	private static class MD5CalculatingHttpServlet extends HttpServlet {
		public MD5CalculatingHttpServlet() {

		}

		@Override
		protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			final MD5CalculatingOutputStream outputStream;

			try {
				outputStream = new MD5CalculatingOutputStream();
			} catch (NoSuchAlgorithmException e) {
				resp.sendError(500);
				return;
			}


			byte[] data = new byte[4096];
			int bytesRead;
			while ((bytesRead = req.getInputStream().read(data, 0, data.length)) >= 0) {
				outputStream.write(data, 0, bytesRead);
			}

			byte[] receivedMD5 = outputStream.getMD5();


			resp.getOutputStream().write(receivedMD5);

			resp.setStatus(200);

		}
	}


}
