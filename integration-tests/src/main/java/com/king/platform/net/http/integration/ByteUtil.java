package com.king.platform.net.http.integration;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteUtil {
	public static void writeTo(ByteBuffer buffer, OutputStream out)
	{
		try {
			if (buffer.hasArray()) {
				out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
				buffer.position(buffer.position() + buffer.remaining());
			} else {
				byte[] bytes = new byte[1024];
				while (buffer.hasRemaining()) {
					int byteCountToWrite = Math.min(buffer.remaining(), 1024);
					buffer.get(bytes, 0, byteCountToWrite);
					out.write(bytes, 0, byteCountToWrite);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
