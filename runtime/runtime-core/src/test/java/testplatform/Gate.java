package testplatform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Blocks asynchronous test-module work until the test opens the gate. */
public final class Gate {

    private static volatile CountDownLatch latch = new CountDownLatch(1);

    private Gate() {
    }

    public static void reset() {
        latch = new CountDownLatch(1);
    }

    public static void open() {
        latch.countDown();
    }

    public static void await() {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
