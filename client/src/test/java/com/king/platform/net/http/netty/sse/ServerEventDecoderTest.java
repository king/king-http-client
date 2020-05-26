// Copyright (C) king.com Ltd 2016
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.sse;

import com.king.platform.net.http.SseClientCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServerEventDecoderTest {
	private CapturingSseExecutionCallback sseCallback;

	@BeforeEach
	public void setUp() throws Exception {
		sseCallback = new CapturingSseExecutionCallback();
	}

	@Test
	public void simpleDataEvent() throws Exception {

		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("data: test\n\n"));

		assertEquals(1, sseCallback.count);
		Event event = sseCallback.poll();

		assertNotNull(event);
		assertNull(event.event);
		assertNull(event.lastSentId);
		assertEquals("test", event.data);
	}

	@Test
	public void endOfLines() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("data: test1\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test2\r\r"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test3\r\n\r\n"));

		assertEquals(3, sseCallback.count);


		Event event1 = sseCallback.poll();
		Event event2 = sseCallback.poll();
		Event event3 = sseCallback.poll();

		assertEquals("test1", event1.data);
		assertEquals("test2", event2.data);
		assertEquals("test3", event3.data);


	}

	@Test
	public void twoDataEvents() throws Exception {

		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("data: test\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test\n\n"));

		assertEquals(2, sseCallback.count);

		Event event = sseCallback.poll();

		assertNotNull(event);
		assertNull(event.event);
		assertNull(event.lastSentId);
		assertEquals("test", event.data);

		Event event2 = sseCallback.poll();
		assertNotNull(event2);

		assertEquals(event, event2);
	}

	@Test
	public void differentFormattedDataEvents() throws Exception {

		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("data:test1\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data :test2\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test3\n\n"));


		assertEquals(3, sseCallback.count);

		Event event = sseCallback.poll();

		assertNotNull(event);
		assertNull(event.event);
		assertNull(event.lastSentId);
		assertEquals("test1", event.data);

		Event event2 = sseCallback.poll();
		Event event3 = sseCallback.poll();

		assertEquals("test2", event2.data);
		assertEquals("test3", event3.data);

	}

	@Test
	public void emptyLinesShouldNotTriggerEvents() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));

		assertEquals(0, sseCallback.count);
	}

	@Test
	public void partialEmitOfDataEvents() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("da"));
		serverEventDecoder.onReceivedContentPart(buffer("ta"));
		serverEventDecoder.onReceivedContentPart(buffer(":"));
		serverEventDecoder.onReceivedContentPart(buffer(" te"));
		serverEventDecoder.onReceivedContentPart(buffer("st"));
		serverEventDecoder.onReceivedContentPart(buffer("\r\n"));
		assertEquals(0, sseCallback.count);
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		assertEquals(1, sseCallback.count);


		Event event = sseCallback.poll();

		assertNotNull(event);
		assertNull(event.event);
		assertNull(event.lastSentId);
		assertEquals("test", event.data);
	}

	@Test
	public void eventName() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("event: testEvent\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test\n"));
		serverEventDecoder.onReceivedContentPart(buffer("\n"));
		assertEquals(1, sseCallback.count);
		Event event = sseCallback.poll();

		assertNotNull(event);
		assertEquals("testEvent", event.event);
		assertEquals("test", event.data);
		assertNull(event.lastSentId);
	}

	@Test
	public void dataWithNewLineInOneEvent() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("event: some_event\r\ndata: data1\r\ndata: data2\r\n\r\n"));

		assertEquals(1, sseCallback.count);
		Event event = sseCallback.poll();

		assertNotNull(event);
		assertEquals("some_event", event.event);
		assertEquals("data1\ndata2", event.data);
		assertNull(event.lastSentId);

	}


	@Test
	public void id() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("id: 1\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test\n\n"));

		Event event = sseCallback.poll();

		assertNotNull(event);
		assertNull(event.event);
		assertEquals("1", event.lastSentId);
		assertEquals("test", event.data);
	}

	@Test
	public void lastSentIdIsReused() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);
		serverEventDecoder.onReceivedContentPart(buffer("id: 1\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test1\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test2\n\n"));

		Event event1 = sseCallback.poll();

		assertNotNull(event1);
		assertNull(event1.event);
		assertEquals("1", event1.lastSentId);
		assertEquals("test1", event1.data);

		Event event2 = sseCallback.poll();

		assertEquals("1", event2.lastSentId);
		assertEquals("test2", event2.data);


		serverEventDecoder.onReceivedContentPart(buffer("id: 2\n"));
		serverEventDecoder.onReceivedContentPart(buffer("data: test3\n\n"));


		Event event3 = sseCallback.poll();

		assertEquals("2", event3.lastSentId);
		assertEquals("test3", event3.data);

	}


	@Test
	public void eventIdData() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);

		serverEventDecoder.onReceivedContentPart(buffer("event: testEvent1\ndata: test1\nid: 1\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("event: testEvent2\ndata: test2\nid: 2\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("event: testEvent3\ndata: test3\nid: 3\n\n"));


		assertEquals(3, sseCallback.count);


		Event event1 = sseCallback.poll();
		Event event2 = sseCallback.poll();
		Event event3 = sseCallback.poll();


		assertEquals("1", event1.lastSentId);
		assertEquals("2", event2.lastSentId);
		assertEquals("3", event3.lastSentId);


		assertEquals("test1", event1.data);
		assertEquals("test2", event2.data);
		assertEquals("test3", event3.data);


		assertEquals("testEvent1", event1.event);
		assertEquals("testEvent2", event2.event);
		assertEquals("testEvent3", event3.event);


	}

	@Test
	public void verifyBrokenUpCRLF() throws Exception {
		ServerEventDecoder serverEventDecoder = new ServerEventDecoder(sseCallback);

		serverEventDecoder.onReceivedContentPart(buffer("event: ClientCommand\r\ndata: test1\r\n\n"));
		serverEventDecoder.onReceivedContentPart(buffer("event: ClientCommand\r"));
		serverEventDecoder.onReceivedContentPart(buffer("\ndata: test2\r\n\n"));

		assertEquals(2, sseCallback.count);

		Event event1 = sseCallback.poll();
		Event event2 = sseCallback.poll();

		assertEquals("ClientCommand", event1.event);
		assertEquals("ClientCommand", event2.event);
	}

	private ByteBuf buffer(String s) throws UnsupportedEncodingException {
		return Unpooled.wrappedBuffer(s.getBytes("UTF-8"));

	}



	private static class CapturingSseExecutionCallback implements SseClientCallback {
		private Queue<Event> events = new ArrayDeque<>();
		int count;

		@Override
		public void onConnect() {

		}

		@Override
		public void onDisconnect() {

		}

		@Override
		public void onError(Throwable throwable) {

		}

		@Override
		public void onEvent(String lastSentId, String event, String data) {
			events.add(new Event(lastSentId, event, data));
			count++;
		}

		public Event poll() {
			return events.poll();
		}
	}

	private static final class Event {
		private String lastSentId;
		private String event;
		private String data;

		public Event(String lastSentId, String event, String data) {
			this.lastSentId = lastSentId;
			this.event = event;
			this.data = data;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Event event1 = (Event) o;

			if (lastSentId != null ? !lastSentId.equals(event1.lastSentId) : event1.lastSentId != null)
				return false;
			if (event != null ? !event.equals(event1.event) : event1.event != null)
				return false;
			return data != null ? data.equals(event1.data) : event1.data == null;

		}

		@Override
		public int hashCode() {
			int result = lastSentId != null ? lastSentId.hashCode() : 0;
			result = 31 * result + (event != null ? event.hashCode() : 0);
			result = 31 * result + (data != null ? data.hashCode() : 0);
			return result;
		}
	}

}
