package com.king.platform.net.http.netty.requestbuilder;


import com.king.platform.net.http.UploadCallback;
import com.king.platform.net.http.netty.eventbus.Event1;
import com.king.platform.net.http.netty.eventbus.Event2;

import java.util.concurrent.Executor;

public class UploadCallbackInvoker {
	private final UploadCallback uploadCallback;
	private final Executor callbackExecutor;
	private long contentLength;
	private long writeLength;


	public UploadCallbackInvoker(UploadCallback uploadCallback, Executor callbackExecutor) {
		this.uploadCallback = uploadCallback;
		this.callbackExecutor = callbackExecutor;
	}


	public void onUploadComplete(Event1<Void> event1, Void v) {
		callbackExecutor.execute(() -> uploadCallback.onCompletedUpload(writeLength, contentLength));
	}


	public void onUploadStarted(Event1<Long> event1, long contentLength) {
		this.contentLength = contentLength;
		callbackExecutor.execute(() -> uploadCallback.onStartedUpload(contentLength));
	}

	public void onUploadProgressed(Event2<Long, Long> event2, long progress, long total) {
		if (contentLength <= 0) {
			callbackExecutor.execute(() -> uploadCallback.onProgress(-1, progress, -1));
		} else {
			int prev = Math.round(((float) writeLength / (float) contentLength) * 100f);

			writeLength = progress;

			int current = Math.round(((float) writeLength / (float) contentLength) * 100f);

			if (current != prev) {
				callbackExecutor.execute(() -> uploadCallback.onProgress(current, writeLength, contentLength));
			}
		}
	}
}
