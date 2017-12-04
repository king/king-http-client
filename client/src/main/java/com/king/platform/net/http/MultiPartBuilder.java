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

	/**
	 * Specify a custom boundary.
	 *
	 * @param boundary the boundary data
	 * @return this builder
	 */
	public MultiPartBuilder boundary(byte[] boundary) {
		this.boundary = boundary;
		return this;
	}

	/**
	 * Add a multi part entry to this builder
	 * Use {@link #create(String, File)}, {@link #create(String, byte[])}, {@link #create(String, InputStream)} or {@link #create(String, String, Charset)} to
	 * create the part object.
	 * @param multiPart the part
	 * @return this builder
	 */
	public MultiPartBuilder addPart(MultiPart multiPart) {
		partBodies.add(multiPart.partBody);
		return this;
	}

	/**
	 * Build the MultiPart body
	 * @return the built multi part
	 */
	public BuiltMultiPart build() {
		ArrayList<MultiPartEntry> multiPartEntries = new ArrayList<>();

		for (PartBody partBody : partBodies) {
			multiPartEntries.add(new MultiPartEntry(partBody, boundary));
		}

		return new BuiltMultiPart(multiPartEntries, boundary);
	}

	/**
	 * Create a byte[] part entry
	 * @param name the name of the part
	 * @param content the content
	 * @return the multi part
	 */
	public static MultiPart create(String name, byte[] content) {
		ByteArrayPartBody partBody = new ByteArrayPartBody(content);
		return new MultiPart(name, partBody);
	}

	/**
	 * Create a File part entry
	 * @param name the name of the part
	 * @param file the file
	 * @return the multi part
	 */
	public static MultiPart create(String name, File file) {
		return new MultiPart(name, new FilePartBody(file));
	}

	/**
	 * Create a InputStream part
	 * @param name the name of the part
	 * @param inputStream the inputStream
	 * @return the multi part
	 */
	public static MultiPart create(String name, InputStream inputStream) {
		return new MultiPart(name, new InputStreamPartBody(inputStream));
	}

	/**
	 * Create a String part entry
	 * @param name the name ot the part
	 * @param content the string content
	 * @param charset the encoding of the content
	 * @return the multi part
	 */
	public static MultiPart create(String name, String content, Charset charset) {
		byte[] bytes = content.getBytes(charset);
		ByteArrayPartBody partBody = new ByteArrayPartBody(bytes);
		partBody.setCharset(charset);
		return new MultiPart(name, partBody);
	}

	public static class MultiPart {
		private final AbstractPartBody partBody;

		private MultiPart(String name, AbstractPartBody partBody) {
			this.partBody = partBody;
			partBody.setName(name);
		}

		/**
		 * Specify the content-type of this part
		 * @param contentType the content type
		 * @return this multi part
		 */
		public MultiPart contentType(String contentType) {
			partBody.setContentType(contentType);
			return this;
		}

		/**
		 * Specify the encoding of this part
		 * @param charset the encoding charset
		 * @return this multi part
		 */
		public MultiPart charset(Charset charset) {
			partBody.setCharset(charset);
			return this;
		}

		/**
		 * Specify the transfer-encoding of this part
		 * @param transferEncoding the transfer encoding
		 * @return this multi part
		 */
		public MultiPart transferEncoding(String transferEncoding) {
			partBody.setTransferEncoding(transferEncoding);
			return this;
		}

		/**
		 * Specify the content id of this part
		 * @param contentId the content-id
		 * @return this multi part
		 */
		public MultiPart contentId(String contentId) {
			partBody.setContentId(contentId);
			return this;
		}

		/**
		 * Specify the disposition-type of this part
		 * @param dispositionType the disposition type
		 * @return this multi part
		 */
		public MultiPart dispositionType(String dispositionType) {
			partBody.setDispositionType(dispositionType);
			return this;
		}

		/**
		 * Add a custom header for this part
		 * @param customHeader the custom header
		 * @return this multi part
		 */
		public MultiPart addCustomHeader(Param customHeader) {
			partBody.addCustomHeader(customHeader);
			return this;
		}

		/**
		 * Specify the filename of this part
		 * @param fileName the filename
		 * @return this multi part
		 */
		public MultiPart fileName(String fileName) {
			partBody.setFileName(fileName);
			return this;
		}
	}

}
