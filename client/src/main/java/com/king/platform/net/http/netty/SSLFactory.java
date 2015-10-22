// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;


import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SSLFactory {
	private final SSLContext sslContext;
	private final boolean acceptAnyCertificate;


	public SSLFactory(boolean acceptAnyCertificate) throws NoSuchAlgorithmException, KeyManagementException {
		this.acceptAnyCertificate = acceptAnyCertificate;
		if (acceptAnyCertificate) {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{new LooseTrustManager()}, new SecureRandom());
		} else {
			sslContext = SSLContext.getDefault();
		}
	}

	public SSLEngine newSSLEngine(String peerHost, int peerPort) throws GeneralSecurityException {

		SSLEngine sslEngine = sslContext.createSSLEngine(peerHost, peerPort);

		if (acceptAnyCertificate) {
			SSLParameters params = sslEngine.getSSLParameters();
			params.setEndpointIdentificationAlgorithm("HTTPS");

			sslEngine.setSSLParameters(params);
		}
		sslEngine.setUseClientMode(true);

		return sslEngine;
	}

	private static class LooseTrustManager implements X509TrustManager {

		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
	}

}
