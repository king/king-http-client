package com.king.platform.net.http;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class HttpResponseTest {
	@Test
	public void getHeaderShouldBeCaseInsensitive() throws Exception {
		List<Map.Entry<String, String>> entries = new ArrayList<>();
		entries.add(new AbstractMap.SimpleEntry<>("Accept", "*/*"));

		HttpResponse httpResponse = new HttpResponse(200, null, entries);

		assertEquals("*/*", httpResponse.getHeader("Accept"));
		assertEquals("*/*", httpResponse.getHeader("ACCEPT"));
		assertEquals("*/*", httpResponse.getHeader("accept"));

	}

	@Test
	public void getHeadersShouldBeCaseInsensitive() throws Exception {

		List<Map.Entry<String, String>> entries = new ArrayList<>();
		entries.add(new AbstractMap.SimpleEntry<>("Accept", "*/*"));
		entries.add(new AbstractMap.SimpleEntry<>("ACCEPT", "*/*"));
		entries.add(new AbstractMap.SimpleEntry<>("accept", "*/*"));

		HttpResponse httpResponse = new HttpResponse(200, null, entries);
		List<String> headers = httpResponse.getHeaders("accept");
		assertEquals(3, headers.size());
		for (String header : headers) {
			assertEquals("*/*", header);
		}
	}


	@Test
	public void getAllHeaders() throws Exception {
		List<Map.Entry<String, String>> entries = new ArrayList<>();
		entries.add(new AbstractMap.SimpleEntry<>("Accept", "*/*"));
		entries.add(new AbstractMap.SimpleEntry<>("ACCEPT", "*/*"));
		entries.add(new AbstractMap.SimpleEntry<>("accept", "*/*"));

		HttpResponse httpResponse = new HttpResponse(200, null, entries);
		Map<String,String> allHeaders = httpResponse.getAllHeaders();
		assertEquals(3, allHeaders.size());
		for (String header : allHeaders.values()) {
			assertEquals("*/*", header);
		}

	}
}
