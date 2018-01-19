package com.king.platform.net.http.netty.eventbus;

import com.king.platform.net.http.netty.HttpRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DefaultEventBusTest {
	@Test
	public void correctSizeAfterCreatingRequestBussesWithPermament() throws Exception {
		DefaultEventBus rootEventBus = new DefaultEventBus();

		rootEventBus.subscribePermanently(Event.ERROR, new ERROR());
		rootEventBus.subscribePermanently(Event.COMPLETED, new COMPLETED());

		for (int i = 0; i < 100; i++) {
			RequestEventBus requestEventBus1 = rootEventBus.createRequestEventBus();
			requestEventBus1.subscribePermanently(Event.COMPLETED, new COMPLETEDREQUEST());
		}

		RequestEventBus requestEventBus1 = rootEventBus.createRequestEventBus();
		requestEventBus1.subscribePermanently(Event.COMPLETED, new COMPLETEDREQUEST());


		assertEquals(1, rootEventBus.getPersistentEvent1Callbacks().get(Event.COMPLETED).size());

	}

	private static class ERROR implements EventBusCallback2<HttpRequestContext, Throwable> {
		@Override
		public void onEvent(HttpRequestContext payload1, Throwable payload2) {
		}
	}

	private static class COMPLETED implements EventBusCallback1<HttpRequestContext> {
		@Override
		public void onEvent(HttpRequestContext payload) {
		}
	}

	private static class COMPLETEDREQUEST implements EventBusCallback1<HttpRequestContext> {
		@Override
		public void onEvent(HttpRequestContext payload) {
		}
	}
}
