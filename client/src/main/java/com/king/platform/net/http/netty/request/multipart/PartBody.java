package com.king.platform.net.http.netty.request.multipart;


import com.king.platform.net.http.util.Param;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public interface PartBody {

	void writeContent(ChannelHandlerContext ctx, boolean isSecure, TotalProgressionTracker totalProgressionTracker) throws IOException;

	long getContentLength();

	String getContentType();

	String getName();

	String getFileName();

	Charset getCharset();

	String getTransferEncoding();

	String getContentId();

	String getDispositionType();

	List<Param> getCustomHeaders();


}
