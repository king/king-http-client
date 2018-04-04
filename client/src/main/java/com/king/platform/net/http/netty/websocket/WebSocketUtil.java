package com.king.platform.net.http.netty.websocket;

import com.king.platform.net.http.netty.ServerInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static io.netty.handler.codec.http.HttpHeaderNames.SEC_WEBSOCKET_VERSION;

public class WebSocketUtil {
	public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";


	private static final FastThreadLocal<MessageDigest> SHA1 = new FastThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() throws Exception {
			try {
				return MessageDigest.getInstance("SHA1");
			} catch (NoSuchAlgorithmException e) {
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

	public static void populateHeaders(ServerInfo serverInfo, HttpHeaders headers) {
		byte[] nonce = randomBytes(16);
		String key = base64(nonce);

		headers.set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET)
			.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE)
			.set(HttpHeaderNames.SEC_WEBSOCKET_KEY, key)
			.set(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, websocketOriginValue(serverInfo.getHost(), serverInfo.getPort()))
			.set(SEC_WEBSOCKET_VERSION, "13");

	}

	public static byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];

		for (int index = 0; index < size; index++) {
			bytes[index] = (byte) randomNumber(0, 255);
		}

		return bytes;
	}

	public static String base64(byte[] data) {
		ByteBuf encodedData = Unpooled.wrappedBuffer(data);
		ByteBuf encoded = io.netty.handler.codec.base64.Base64.encode(encodedData);
		String encodedString = encoded.toString(CharsetUtil.UTF_8);
		encoded.release();
		return encodedString;
	}

	private static String websocketOriginValue(String host, int wsPort) {
		String originValue = (wsPort == HttpScheme.HTTPS.port() ?
			HttpScheme.HTTPS.name() : HttpScheme.HTTP.name()) + "://" + host;
		if (wsPort != HttpScheme.HTTP.port() && wsPort != HttpScheme.HTTPS.port()) {
			// if the port is not standard (80/443) its needed to add the port to the header.
			// See http://tools.ietf.org/html/rfc6454#section-6.2
			return originValue + ':' + wsPort;
		}
		return originValue;
	}

	private static int randomNumber(int minimum, int maximum) {
		return (int) (Math.random() * maximum + minimum);
	}
}
