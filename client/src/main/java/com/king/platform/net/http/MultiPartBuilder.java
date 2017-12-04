package com.king.platform.net.http;


import com.king.platform.net.http.netty.request.multipart.*;
import com.king.platform.net.http.util.Param;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class MultiPartBuilder {
	private static byte[] BOUNDARY_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.US_ASCII);

	private byte[] boundary;

	private List<PartBody> partBodies = new ArrayList<>();

	public MultiPartBuilder() {
		boundary = generateBoundary();
	}

	private byte[] generateBoundary() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		byte[] bytes = new byte[40];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = BOUNDARY_CHARS[random.nextInt(BOUNDARY_CHARS.length)];
		}
		return bytes;
	}

	public MultiPartBuilder setBoundary(byte[] boundary) {
		this.boundary = boundary;
		return this;
	}

	public MultiPartBuilder addPart(MultiPart multiPart) {
		partBodies.add(multiPart.partBody);
		return this;
	}

	public BuiltMultiPart build() {
		ArrayList<MultiPartEntry> multiPartEntries = new ArrayList<>();

		for (PartBody partBody : partBodies) {
			multiPartEntries.add(new MultiPartEntry(partBody, boundary));
		}

		return new BuiltMultiPart(multiPartEntries, boundary);
	}


	public static MultiPart create(String name, byte[] content) {
		ByteArrayPartBody partBody = new ByteArrayPartBody(content);
		return new MultiPart(name, partBody);
	}

	public static MultiPart create(String name, File file) {
		return new MultiPart(name, new FilePartBody(file));
	}

	public static MultiPart create(String name, InputStream inputStream) {
		return new MultiPart(name, new InputStreamPartBody(inputStream));
	}

	public static MultiPart create(String name, String content, Charset charset) {
		byte[] bytes = content.getBytes(charset);
		ByteArrayPartBody partBody = new ByteArrayPartBody(bytes);
		partBody.setCharset(charset);
		return new MultiPart(name, partBody);
	}

	public static class MultiPart {
		private final AbstractPartBody partBody;

		public MultiPart(String name, AbstractPartBody partBody) {
			this.partBody = partBody;
			partBody.setName(name);
		}

		public MultiPart contentType(String contentType) {
			partBody.setContentType(contentType);
			return this;
		}

		public MultiPart charset(Charset charset) {
			partBody.setCharset(charset);
			return this;
		}

		public MultiPart transferEncoding(String transferEncoding) {
			partBody.setTransferEncoding(transferEncoding);
			return this;
		}

		public MultiPart contentId(String contentId) {
			partBody.setContentId(contentId);
			return this;
		}

		public MultiPart dispositionType(String dispositionType) {
			partBody.setDispositionType(dispositionType);
			return this;
		}

		public MultiPart addCustomHeader(Param customHeader) {
			partBody.addCustomHeader(customHeader);
			return this;
		}

		public MultiPart fileName(String fileName) {
			partBody.setFileName(fileName);
			return this;
		}
	}

}
