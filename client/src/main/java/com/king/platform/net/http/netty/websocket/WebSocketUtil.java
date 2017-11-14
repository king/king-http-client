package com.king.platform.net.http.netty.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketUtil {
	public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private static final FastThreadLocal<MessageDigest> MD5 = new FastThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() throws Exception {
			try {
				//Try to get a MessageDigest that uses MD5
				return MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				//This shouldn't happen! How old is the computer?
				throw new InternalError("MD5 not supported on this platform - Outdated?");
			}
		}
	};

	private static final FastThreadLocal<MessageDigest> SHA1 = new FastThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() throws Exception {
			try {
				//Try to get a MessageDigest that uses SHA1
				return MessageDigest.getInstance("SHA1");
			} catch (NoSuchAlgorithmException e) {
				//This shouldn't happen! How old is the computer?
				throw new InternalError("SHA-1 not supported on this platform - Outdated?");
			}
		}
	};

	public static byte[] sha1(byte[] data) {
		return digest(SHA1, data);
	}

	private static byte[] digest(FastThreadLocal<MessageDigest> digestFastThreadLocal, byte[] data) {
		MessageDigest digest = digestFastThreadLocal.get();
		digest.reset();
		return digest.digest(data);
	}


	public static String getAcceptKey(String key) {
		Base64.Encoder encoder = Base64.getEncoder();
		byte[] bytes = (key + MAGIC_GUID).getBytes(StandardCharsets.US_ASCII);
		byte[] encode = encoder.encode(digest(SHA1, bytes));
		return new String(encode, StandardCharsets.US_ASCII);
	}

	public static String base64(byte[] data) {
		ByteBuf encodedData = Unpooled.wrappedBuffer(data);
		ByteBuf encoded = io.netty.handler.codec.base64.Base64.encode(encodedData);
		String encodedString = encoded.toString(CharsetUtil.UTF_8);
		encoded.release();
		return encodedString;
	}

	public static byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];

		for (int index = 0; index < size; index++) {
			bytes[index] = (byte) randomNumber(0, 255);
		}

		return bytes;
	}

	private static int randomNumber(int minimum, int maximum) {
		return (int) (Math.random() * maximum + minimum);
	}

}
