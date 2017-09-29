package com.king.platform.net.http;


public interface UploadCallbackAdapter extends UploadCallback {
	@Override
	default void onStartedUpload(long contentLength) {

	}

	@Override
	default void onProgress(int percentageCompleted, long writeLength, long contentLength) {

	}

	@Override
	default void onCompletedUpload(long writeLength, long contentLength) {

	}
}
