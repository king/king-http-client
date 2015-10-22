// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http;


import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class FileResponseConsumer implements  ResponseBodyConsumer<File>  {
	private final File file;
	private FileOutputStream fileOutputStream;
	private WritableByteChannel channel;

	public FileResponseConsumer(File file) {
		this.file = file;
	}

	@Override
	public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {
		fileOutputStream = new FileOutputStream(file);
		channel = Channels.newChannel(fileOutputStream);
	}

	@Override
	public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		channel.write(buffer);
	}

	@Override
	public void onCompletedBody() throws Exception {
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	@Override
	public File getBody() {
		return file;
	}
}
