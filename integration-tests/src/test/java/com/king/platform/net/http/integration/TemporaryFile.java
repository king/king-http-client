// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.integration;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class TemporaryFile {
	private final TemporaryFolder folder;
	private File file;
	private byte[] fileMd5;

	public TemporaryFile(TemporaryFolder folder) {
		this.folder = folder;
	}

	public void generateContent(int sizeInKb) throws IOException, NoSuchAlgorithmException {
		file = File.createTempFile("junit", null, folder.getRoot());

		MD5CalculatingOutputStream outputStream = new MD5CalculatingOutputStream(new FileOutputStream(file));

		Random random = new Random();
		byte[] buffer = new byte[1024];
		for (int i = 0; i < sizeInKb; i++) {
			random.nextBytes(buffer);
			outputStream.write(buffer);
		}

		outputStream.flush();
		outputStream.close();

		fileMd5 = outputStream.getMD5();
	}

	public File getFile() {
		return file;
	}

	public byte[] getFileMd5() {
		return fileMd5;
	}

	public File getTempFile() throws IOException {
		return File.createTempFile("junit", null, folder.getRoot());
	}
}
