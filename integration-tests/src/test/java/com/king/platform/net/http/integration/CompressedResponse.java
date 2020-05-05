// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;


import com.king.platform.net.http.ByteArrayResponseBodyConsumer;
import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.HttpResponse;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompressedResponse {
	IntegrationServer integrationServer;
	private HttpClient httpClient;
	private int port;

	private String okBody = "Morbi ut pretium augue, eu fringilla tortor. Nulla quis feugiat velit. Proin iaculis justo ut gravida cursus. Fusce nec posuere " +
		"nibh. Donec suscipit orci quis est luctus porta. Cras eget nulla justo. Quisque ut nibh ac ligula fringilla mattis. Donec posuere, urna quis " +
		"condimentum egestas, justo nibh consequat odio, iaculis porta tellus mi vitae ex. Morbi consectetur massa eget mi maximus, ut sodales eros " +
		"vehicula. Aliquam dignissim volutpat massa, a venenatis elit commodo tempor. Aliquam laoreet nisl non mauris lacinia, porttitor euismod tortor " +
		"tristique. Morbi aliquam diam a elementum semper. Cras et tincidunt nulla, eget fermentum diam. Aliquam erat volutpat.";

	@BeforeEach
	public void setUp() throws Exception {
		integrationServer = new JettyIntegrationServer();
		integrationServer.start();
		port = integrationServer.getPort();

		FilterHolder gzipHolder = new FilterHolder(new GzipFilter());
		gzipHolder.setInitParameter("mimeTypes", "text/plain");
		gzipHolder.setInitParameter("minGzipSize", "1");
		gzipHolder.setInitParameter("methods", "GET,POST");

		integrationServer.addFilter(gzipHolder, "/*");

		httpClient = new TestingHttpClientFactory().useChannelPool().create();
		httpClient.start();

		integrationServer.addServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				byte[] compress = compress(okBody.getBytes(StandardCharsets.UTF_8));
				resp.setHeader("content-encoding", "gzip");
				resp.setStatus(200);
				resp.setContentLength(compress.length);
				ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.write(compress);
				outputStream.flush();

			}
		}, "/testOk");

	}

	@Test
	public void getLocalWithDecompression() throws Exception {
		HttpResponse<byte[]> httpResponse = getDeflatedResponse();

		verifyDeflated(httpResponse);
	}

	@Test
	public void getLocalWithDecompressionTwice() throws Exception {
		HttpResponse<byte[]> httpResponse = getDeflatedResponse();

		verifyDeflated(httpResponse);

		Thread.sleep(10);

		httpResponse = getDeflatedResponse();


		verifyDeflated(httpResponse);


	}


	@Test
	public void getLocalWithoutDecompression() throws Exception {

		HttpResponse<byte[]> httpResponse = getCompressedResponse();

		verifyCompressed(httpResponse);

	}

	@Test
	public void getMultipleOnSameChannel() throws Exception {
		HttpResponse<byte[]> httpResponse = getDeflatedResponse();

		verifyDeflated(httpResponse);

		Thread.sleep(10);


		httpResponse = getCompressedResponse();


		verifyCompressed(httpResponse);

		Thread.sleep(10);

		httpResponse = getDeflatedResponse();


		verifyDeflated(httpResponse);


		Thread.sleep(10);

		httpResponse = getDeflatedResponse();


		verifyDeflated(httpResponse);


	}

	private HttpResponse<byte[]> getCompressedResponse() {
		HttpResponse<byte[]> httpResponse;
		httpResponse = httpClient.createGet("http://localhost:" + port + "/testOk")
			.acceptCompressedResponse(true)
			.automaticallyDecompressResponse(false)
			.build(ByteArrayResponseBodyConsumer::new)
			.execute()
			.join();
		return httpResponse;
	}

	private HttpResponse<byte[]> getDeflatedResponse() {
		HttpResponse<byte[]> httpResponse;
		httpResponse = httpClient.createGet("http://localhost:" + port + "/testOk")
			.acceptCompressedResponse(true)
			.automaticallyDecompressResponse(true)
			.build(ByteArrayResponseBodyConsumer::new)
			.execute()
			.join();
		return httpResponse;
	}

	private void verifyDeflated(HttpResponse<byte[]> httpResponse) {
		assertEquals(okBody, new String(httpResponse.getBody(), StandardCharsets.UTF_8));
		assertEquals("chunked", httpResponse.getHeader("transfer-encoding"));
		assertEquals(200, httpResponse.getStatusCode());
	}


	private void verifyCompressed(HttpResponse<byte[]> httpResponse) throws IOException {
		byte[] body = httpResponse.getBody();
		assertEquals(okBody, new String(deflate(body), StandardCharsets.UTF_8));
		assertEquals("" + body.length, httpResponse.getHeader("content-length"));
		assertEquals(200, httpResponse.getStatusCode());
	}


	private static byte[] compress(byte[] dataToCompress) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(dataToCompress.length);

		GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
		zipStream.write(dataToCompress);
		zipStream.close();
		byteStream.close();

		return byteStream.toByteArray();
	}

	private static byte[] deflate(byte[] dataToDeflate) throws IOException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(dataToDeflate);
		GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;

		while ((len = gzipInputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, len);
		}
		return outputStream.toByteArray();
	}

	@AfterEach
	public void tearDown() throws Exception {
		integrationServer.shutdown();
		httpClient.shutdown();

	}


}
