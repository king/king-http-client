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


	public MultiPartBuilder createPart(String name, byte[] content, MultiPartHeaderConfigurer multiPartHeaderConfigurer) {
		ByteArrayPartBody partBody = new ByteArrayPartBody(content);
		multiPartHeaderConfigurer.configure(new MultiPartHeader(name, partBody));
		partBodies.add(partBody);
		return this;
	}

	public MultiPartBuilder createPart(String name, File file, MultiPartHeaderConfigurer multiPartHeaderConfigurer) {
		FilePartBody partBody = new FilePartBody(file);
		multiPartHeaderConfigurer.configure(new MultiPartHeader(name, partBody));
		partBodies.add(partBody);
		return this;
	}

	public MultiPartBuilder createPart(String name, InputStream inputStream, MultiPartHeaderConfigurer multiPartHeaderConfigurer) {
		InputStreamPartBody partBody = new InputStreamPartBody(inputStream);
		multiPartHeaderConfigurer.configure(new MultiPartHeader(name, partBody));
		partBodies.add(partBody);
		return this;
	}

	public MultiPartBuilder createPart(String name, String content, Charset charset, MultiPartHeaderConfigurer multiPartHeaderConfigurer) {
		byte[] bytes = content.getBytes(charset);
		ByteArrayPartBody partBody = new ByteArrayPartBody(bytes);
		partBody.setCharset(charset);
		multiPartHeaderConfigurer.configure(new MultiPartHeader(name, partBody));
		partBodies.add(partBody);
		return this;
	}


	public interface MultiPartHeaderConfigurer {
		void configure(MultiPartHeader multiPartHeader);
	}

	public BuiltMultiPart build() {
		ArrayList<MultiPartEntry> multiPartEntries = new ArrayList<>();

		for (PartBody partBody : partBodies) {
			multiPartEntries.add(new MultiPartEntry(partBody, boundary));
		}

		return new BuiltMultiPart(multiPartEntries, boundary);
	}

	public static class MultiPartHeader {
		private final AbstractPartBody partBody;

		public MultiPartHeader(String name, AbstractPartBody partBody) {
			this.partBody = partBody;
			partBody.setName(name);
		}

		public MultiPartHeader contentType(String contentType) {
			partBody.setContentType(contentType);
			return this;
		}

		public MultiPartHeader charset(Charset charset) {
			partBody.setCharset(charset);
			return this;
		}

		public MultiPartHeader transferEncoding(String transferEncoding) {
			partBody.setTransferEncoding(transferEncoding);
			return this;
		}

		public MultiPartHeader contentId(String contentId) {
			partBody.setContentId(contentId);
			return this;
		}

		public MultiPartHeader dispositionType(String dispositionType) {
			partBody.setDispositionType(dispositionType);
			return this;
		}

		public MultiPartHeader addCustomHeader(Param customHeader) {
			partBody.addCustomHeader(customHeader);
			return this;
		}

		public MultiPartHeader fileName(String fileName) {
			partBody.setFileName(fileName);
			return this;
		}
	}

}
