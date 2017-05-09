package com.king.platform.net.http.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class UriQueryParserTest {
	private UriQueryParser uriQueryParser;

	@Before
	public void setUp() throws Exception {
		uriQueryParser = new UriQueryParser("http://localhost?param1=value1&param2=value2&param3");
	}

	@Test
	public void getParameters() throws Exception {
		assertEquals(3, uriQueryParser.params().size());

		assertEquals("value1", uriQueryParser.getValue("param1").get(0));
		assertEquals("value2", uriQueryParser.getValue("param2").get(0));
		assertEquals("", uriQueryParser.getValue("param3").get(0));
	}

	@Test
	public void getDefaultValue() throws Exception {
		assertEquals("value1", uriQueryParser.getValueOrDefault("param1", "missingValue"));
		assertEquals("missingValue", uriQueryParser.getValueOrDefault("param4", "missingValue"));
	}
}
