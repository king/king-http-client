package com.king.platform.net.http.netty.request.multipart;


import com.king.platform.net.http.util.Param;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPartBody implements PartBody {
	protected String contentType;
	protected String name;
	protected Charset charset;
	protected String transferEncoding;
	protected String contentId;
	protected String dispositionType;
	protected String fileName;
	protected List<Param> customHeaders = new ArrayList<>();


	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Charset getCharset() {
		return charset;
	}

	@Override
	public String getTransferEncoding() {
		return transferEncoding;
	}

	@Override
	public String getContentId() {
		return contentId;
	}

	@Override
	public String getDispositionType() {
		return dispositionType;
	}

	@Override
	public List<Param> getCustomHeaders() {
		return customHeaders;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public void setTransferEncoding(String transferEncoding) {
		this.transferEncoding = transferEncoding;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public void setDispositionType(String dispositionType) {
		this.dispositionType = dispositionType;
	}

	public void addCustomHeader(Param customHeader) {
		this.customHeaders.add(customHeader);
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
