package com.king.platform.net.http.netty.request.multipart;


import com.king.platform.net.http.util.Param;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class MultiPartEntry {
	private static final byte QUOTE_BYTE = '\"';
	protected static final byte[] CRLF_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);
	protected static final byte[] EXTRA_BYTES = "--".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition: ".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] FORM_DATA_DISPOSITION_TYPE_BYTES = "form-data".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] NAME_BYTES = "; name=".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] CONTENT_TYPE_BYTES = "Content-Type: ".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] CHARSET_BYTES = "; charset=".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] FILE_NAME_BYTES = "; filename=".getBytes(US_ASCII);
	private static final byte[] CONTENT_TRANSFER_ENCODING_BYTES = "Content-Transfer-Encoding: ".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] HEADER_NAME_VALUE_SEPARATOR_BYTES = ": ".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] CONTENT_ID_BYTES = "Content-ID: ".getBytes(StandardCharsets.US_ASCII);


	private final PartBody partBody;
	protected final byte[] boundary;

	private final byte[] preContent;
	private final byte[] postContent = new byte[]{'\r', '\n'};

	public MultiPartEntry(PartBody partBody, byte[] boundary) {
		this.partBody = partBody;
		this.boundary = boundary;

		if (partBody != null) {
			try {
				preContent = calcPreContent();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			preContent = null;
		}
	}

	private byte[] calcPreContent() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
		buffer.write(EXTRA_BYTES);
		buffer.write(boundary);
		writeDispositionHeader(buffer);
		writeContentTypeHeader(buffer);
		writeContentEncodingHeader(buffer);
		writeContentIdHeader(buffer);
		writeCustomHeaders(buffer);
		buffer.write(CRLF_BYTES);
		buffer.write(CRLF_BYTES);
		return buffer.toByteArray();
	}

	private void writeDispositionHeader(ByteArrayOutputStream buffer) throws IOException {
		buffer.write(CRLF_BYTES);
		buffer.write(CONTENT_DISPOSITION_BYTES);

		if (partBody.getDispositionType() != null) {
			buffer.write(partBody.getDispositionType().getBytes(StandardCharsets.US_ASCII));
		} else {
			buffer.write(FORM_DATA_DISPOSITION_TYPE_BYTES);
		}

		if (partBody.getName() != null) {
			buffer.write(NAME_BYTES);
			buffer.write(QUOTE_BYTE);
			buffer.write(partBody.getName()
				.getBytes(StandardCharsets.US_ASCII)); //TODO: this should be escaped according to RFC if the encoding is other then US_ASCII
			buffer.write(QUOTE_BYTE);
		}

		if (partBody.getFileName() != null) {
			buffer.write(FILE_NAME_BYTES);
			buffer.write(QUOTE_BYTE);
			buffer.write(partBody.getFileName()
				.getBytes(StandardCharsets.US_ASCII));  //TODO: this should be escaped according to RFC if the encoding is other then US_ASCII
			buffer.write(QUOTE_BYTE);
		}

	}

	private void writeContentTypeHeader(ByteArrayOutputStream buffer) throws IOException {
		String contentType = partBody.getContentType();
		if (contentType != null) {
			buffer.write(CRLF_BYTES);
			buffer.write(CONTENT_TYPE_BYTES);
			buffer.write(contentType.getBytes(StandardCharsets.US_ASCII));
			Charset charSet = partBody.getCharset();
			if (charSet != null) {
				buffer.write(CHARSET_BYTES);
				buffer.write(charSet.name().getBytes(StandardCharsets.US_ASCII));
			}
		}
	}

	private void writeContentEncodingHeader(ByteArrayOutputStream buffer) throws IOException {
		String transferEncoding = partBody.getTransferEncoding();
		if (transferEncoding != null) {
			buffer.write(CRLF_BYTES);
			buffer.write(CONTENT_TRANSFER_ENCODING_BYTES);
			buffer.write(transferEncoding.getBytes(StandardCharsets.US_ASCII));
		}
	}

	private void writeContentIdHeader(ByteArrayOutputStream buffer) throws IOException {
		String contentId = partBody.getContentId();
		if (contentId != null) {
			buffer.write(CRLF_BYTES);
			buffer.write(CONTENT_ID_BYTES);
			buffer.write(contentId.getBytes(StandardCharsets.US_ASCII));
		}
	}

	private void writeCustomHeaders(ByteArrayOutputStream buffer) throws IOException {
		if (partBody.getCustomHeaders() != null) {
			for (Param param : partBody.getCustomHeaders()) {
				String name = param.getName().toString();
				String value = param.getValue().toString();
				buffer.write(CRLF_BYTES);
				buffer.write(name.getBytes(StandardCharsets.US_ASCII));
				buffer.write(HEADER_NAME_VALUE_SEPARATOR_BYTES);
				buffer.write(value.getBytes(StandardCharsets.US_ASCII));
			}
		}
	}

	public long getContentLength() {
		long contentLength = partBody.getContentLength();
		if (contentLength < 0) {
			return -1;
		}
		return preContent.length + contentLength + postContent.length;
	}

	public ChannelFuture writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException {
		ByteBuf preContentByteBuf = ctx.alloc().buffer(preContent.length).writeBytes(preContent);

		ChannelFuture future = ctx.write(preContentByteBuf, ctx.newProgressivePromise());
		future.addListener(new TotalProgressiveFutureListener(totalProgressionTracker));

		partBody.writeContent(ctx, isSecure, totalProgressionTracker);

		ByteBuf postContentByteBuf = ctx.alloc().buffer(postContent.length).writeBytes(postContent);

		return ctx.writeAndFlush(postContentByteBuf, ctx.newProgressivePromise())
			.addListener(new TotalProgressiveFutureListener(totalProgressionTracker));

	}


}
