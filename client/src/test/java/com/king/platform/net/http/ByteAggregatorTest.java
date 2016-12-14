// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class ByteAggregatorTest {

	@Test
	public void constructorShouldNotThrowExceptionWhenSuppliedWithNegativeLength() throws Exception {
		assertNotNull(new ByteAggregator(-100));
	}
}
