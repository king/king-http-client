package com.king.platform.net.http;


public interface UploadCallback {
	void onStartedUpload(long contentLength);

	void onProgress(int percentageCompleted, long writeLength, long contentLength);

	void onCompletedUpload(long writeLength, long contentLength);
}
