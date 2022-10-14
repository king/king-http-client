// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ByteAggregatorTest {

	@Test
	public void constructorShouldNotThrowExceptionWhenSuppliedWithNegativeLength() {
		assertDoesNotThrow(() -> new ByteAggregator(-100));
	}


	@Test
	@SuppressWarnings("deprecation")
	public void constructorShouldThrowExceptionWhenSuppliedWithABigLongNumber() {
		assertThrows(ArithmeticException.class, () -> new ByteAggregator((long) Integer.MAX_VALUE + 3));
	}
}
