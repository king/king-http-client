// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5CalculatingOutputStream extends OutputStream {
	private final OutputStream wrappedOutputStream;
	private final MessageDigest md;

	public MD5CalculatingOutputStream(OutputStream wrappedOutputStream) throws NoSuchAlgorithmException {
		this.wrappedOutputStream = wrappedOutputStream;
		md = MessageDigest.getInstance("MD5");
	}

	public MD5CalculatingOutputStream() throws NoSuchAlgorithmException {
		this.wrappedOutputStream = null;
		md = MessageDigest.getInstance("MD5");
	}

	@Override
	public void write(int b) throws IOException {
		if (this.wrappedOutputStream != null) {
			this.wrappedOutputStream.write(b);
		}

		this.md.update((byte) b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (this.wrappedOutputStream != null) {
			this.wrappedOutputStream.write(b, off, len);
		}

		this.md.update(b, off, len);
	}

	public byte[] getMD5() {
		return this.md.digest();
	}

	@Override
	public void close() throws IOException {
		if (this.wrappedOutputStream != null) {
			this.wrappedOutputStream.close();
		}
	}

	@Override
	public void flush() throws IOException {
		if (this.wrappedOutputStream != null) {
			wrappedOutputStream.flush();
		}
	}
}
