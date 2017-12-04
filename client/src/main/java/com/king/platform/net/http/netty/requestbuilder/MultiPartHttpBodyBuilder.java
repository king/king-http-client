package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.BuiltMultiPart;
import com.king.platform.net.http.netty.request.HttpBody;
import com.king.platform.net.http.netty.request.multipart.MultiPartHttpBody;

import java.nio.charset.Charset;

public class MultiPartHttpBodyBuilder implements RequestBodyBuilder {
	private final BuiltMultiPart builtMultiPart;

	public MultiPartHttpBodyBuilder(BuiltMultiPart builtMultiPart) {
		this.builtMultiPart = builtMultiPart;
	}

	@Override
	public HttpBody createHttpBody(String contentType, Charset characterEncoding) {
		if (contentType == null) {
			contentType = "multipart/form-data";
		}
		return new MultiPartHttpBody(builtMultiPart.getParts(), contentType, builtMultiPart.getBoundary());
	}
}
