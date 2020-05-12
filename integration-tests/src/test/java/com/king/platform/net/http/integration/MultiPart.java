// Copyright (C) king.com Ltd 2017
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ConfKeys;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.MultiPartBuilder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MultiPart {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	@BeforeEach
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		httpClient = new TestingHttpClientFactory()
			.setOption(ConfKeys.HTTP_CODEC_MAX_CHUNK_SIZE, 1024)
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
				.addPart(MultiPartBuilder.create("text1", "Message 1", StandardCharsets.ISO_8859_1).contentType("multipart/form-data"))
				.addPart(MultiPartBuilder
					.create("binary1", new byte[]{0x00, 0x01, 0x02})
					.contentType("application/octet-stream")
					.charset(StandardCharsets.UTF_8)
					.fileName("application.bin"))
				.addPart(MultiPartBuilder.create("text2", "Message 2", StandardCharsets.ISO_8859_1))
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

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
