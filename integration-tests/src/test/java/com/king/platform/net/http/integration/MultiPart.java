// Copyright (C) king.com Ltd 2017
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.MultiPartBuilder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MultiPart {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "EVERYTHING IS OKAY!";
	private String content = "BODY CONTENT";

	@Before
	public void setUp() throws Exception {
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
	public void postMultiPart() throws Exception {

		AtomicReference<List<FileItem>> partReferences = new AtomicReference<>();
		AtomicReference<Exception> exceptionReference = new AtomicReference<>();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());

				try {
					List<FileItem> fileItems = servletFileUpload.parseRequest(req);
					partReferences.set(fileItems);
				} catch (FileUploadException e) {
					exceptionReference.set(e);
				}


			}
		}, "/testMultiPart");

		httpClient.createPost("http://localhost:" + port + "/testMultiPart")
			.idleTimeoutMillis(0)
			.totalRequestTimeoutMillis(0)
			.content(new MultiPartBuilder()
				.createPart("text1", "Message 1", StandardCharsets.ISO_8859_1, multiPartHeader -> multiPartHeader.contentType("multipart/form-data"))

				.createPart("binary1", new byte[]{0x00, 0x01, 0x02},
					multiPartHeader -> multiPartHeader.contentType("application/octet-stream")
						.charset(StandardCharsets.UTF_8)
						.fileName("application.bin"))

				.createPart("text2", "Message 2", StandardCharsets.ISO_8859_1, multiPartHeader -> multiPartHeader.contentType("multipart/form-data"))

				.build())
			.build()
			.execute()
			.join();

		assertNull(exceptionReference.get());

		List<FileItem> fileItems = partReferences.get();
		FileItem fileItem = fileItems.get(1);
		assertEquals("application/octet-stream; charset=UTF-8", fileItem.getContentType());
		assertEquals("binary1", fileItem.getFieldName());
		assertEquals("application.bin", fileItem.getName());

	}

	@Test
	public void name() throws Exception {

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().write("FOUND PATH");
			}
		}, "/repo/*");

		HttpResponse<String> join = httpClient.createGet("http://localhost:" + port + "/repo/asd")
			.build().execute().join();

		System.out.println(join.getBody());

	}

	@After
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
