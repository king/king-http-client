package com.king.platform.net.http.netty.request.multipart;


import com.king.platform.net.http.netty.request.HttpBody;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressivePromise;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class MultiPartHttpBody implements HttpBody {
	private final List<MultiPartEntry> partBodies;
	private final byte[] boundary;
	private final long contentLength;
	private final String contentType;

	public MultiPartHttpBody(List<MultiPartEntry> partBodies, String contentType, byte[] boundary) {
		this.partBodies = partBodies;
		this.boundary = boundary;

		partBodies.add(new MessageEndMultiPartEntry(boundary));

		contentLength = calculateContentLength();

		this.contentType = buildContentType(contentType);
	}

	private long calculateContentLength() {
		long totalContentLength = 0;
		for (MultiPartEntry partBody : partBodies) {
			long partContentLength = partBody.getContentLength();
			if (partContentLength < 0) {
				return -1;
			}

			totalContentLength += partContentLength;
		}
		return totalContentLength;
	}

	private String buildContentType(String base) {
		StringBuilder buffer = new StringBuilder(base);
		if (base.length() != 0 && base.charAt(base.length() - 1) != ';') {
			buffer.append(';');
		}

		return buffer.append(" boundary=").append(new String(boundary, StandardCharsets.US_ASCII)).toString();

	}

	@Override
	public long getContentLength() {
		return contentLength;
	}

	@Override
	public String getContentType() {

		return contentType;
	}

	@Override
	public Charset getCharacterEncoding() {
		return null;
	}

	@Override
	public ChannelFuture writeContent(ChannelHandlerContext ctx, boolean isSecure) throws IOException {
		ChannelProgressivePromise channelProgressivePromise = ctx.channel().newProgressivePromise();

		TotalProgressionTracker totalProgressionTracker = new TotalProgressionTracker(contentLength, channelProgressivePromise);

		Iterator<MultiPartEntry> iterator = partBodies.iterator();
		writeNext(ctx, totalProgressionTracker, isSecure, iterator);

		return channelProgressivePromise;
	}

	private void writeNext(ChannelHandlerContext ctx, TotalProgressionTracker totalProgressionTracker, boolean isSecure, Iterator<MultiPartEntry> iterator)
		throws IOException {
		if (!iterator.hasNext()) {
			totalProgressionTracker.setSuccess();
			return;
		}

		MultiPartEntry entry = iterator.next();

		System.out.println("Writing " + entry);

		ChannelFuture channelFuture = entry.writeContent(ctx, isSecure, totalProgressionTracker);
		channelFuture.addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				System.out.println("Write success, writing next");
				writeNext(ctx, totalProgressionTracker, isSecure, iterator);
			} else {
				System.out.println("Write failed!" + future.cause());
				totalProgressionTracker.setFailure(future.cause());
			}
		});
	}
}
