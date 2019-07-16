package com.king.platform.net.http.integration;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Md5Util {

	private Md5Util() {}

	static String getChecksum(byte[] model) {
		byte[] rawChecksum = getRawChecksum(model);
		return hexStringFromBytes(rawChecksum);
	}

	static byte[] getRawChecksum(byte[] bytesToHash) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(bytesToHash, 0, bytesToHash.length);
			return md.digest();
		} catch (NoSuchAlgorithmException nse) {
			throw new RuntimeException(nse);
		}
	}

	static String hexStringFromBytes(byte[] b) {
		int outputLength = b.length * 2;
		String formatToHexWithFullLength = "%0" + outputLength + "x";
		return String.format(formatToHexWithFullLength, new BigInteger(1, b));
	}

}
