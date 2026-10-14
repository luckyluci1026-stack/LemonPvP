package testplatform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Blocks test work (e.g. an async task of a module) until the test opens the gate. */
public final class Gate {

    private static volatile CountDownLatch latch = new CountDownLatch(1);
    private static volatile CountDownLatch entered = new CountDownLatch(1);

    private Gate() {
    }

    public static void reset() {
        latch = new CountDownLatch(1);
        entered = new CountDownLatch(1);
    }

    public static void open() {
        latch.countDown();
    }

    public static void await() {
        entered.countDown();
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Waits until some thread is inside {@link #await()}. */
    public static boolean awaitEntered() throws InterruptedException {
        return entered.await(30, TimeUnit.SECONDS);
    }
}
