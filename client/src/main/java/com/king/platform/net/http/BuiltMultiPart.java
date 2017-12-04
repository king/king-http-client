package com.king.platform.net.http;


import com.king.platform.net.http.netty.request.multipart.MultiPartEntry;

import java.util.List;

public class BuiltMultiPart {
	private final List<MultiPartEntry> parts;
	private final byte[] boundary;

	public BuiltMultiPart(List<MultiPartEntry> parts, byte[] boundary) {
		this.parts = parts;
		this.boundary = boundary;
	}

	public List<MultiPartEntry> getParts() {
		return parts;
	}

	public byte[] getBoundary() {
		return boundary;
	}
}
