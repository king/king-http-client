package com.king.platform.net.http.netty.sse;


import java.util.concurrent.CountDownLatch;

public class AwaitLatch {
    private CountDownLatch latch = new CountDownLatch(1);

    public void closed() {
        latch.countDown();
        latch = new CountDownLatch(1);
    }

    public void awaitClose() throws InterruptedException {
        latch.await();
    }


}
