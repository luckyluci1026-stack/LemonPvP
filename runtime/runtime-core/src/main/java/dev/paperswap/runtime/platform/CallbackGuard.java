package dev.paperswap.runtime.platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps every invocation of module code made by the platform (event handlers, task bodies,
 * command executors).
 *
 * <ul>
 *     <li>Calls arriving after the module was deactivated are dropped, which closes the
 *     race between "cancel task / unregister listener" and an invocation that was already
 *     queued.</li>
 *     <li>Exceptions are logged with the module id and never propagate into the server.</li>
 *     <li>The thread context class loader is set to the module class loader for the
 *     duration of the call and restored afterwards.</li>
 *     <li>In-flight invocations are counted so diagnostics can show asynchronous callbacks
 *     that are still running while the module is being disabled.</li>
 * </ul>
 */
public final class CallbackGuard {

    private final String moduleId;
    private final Logger logger;
    private final ClassLoader classLoader;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();

    /**
     * @param moduleId    module id
     * @param logger      module logger
     * @param classLoader module class loader
     */
    public CallbackGuard(String moduleId, Logger logger, ClassLoader classLoader) {
        this.moduleId = moduleId;
        this.logger = logger;
        this.classLoader = classLoader;
    }

    /** Allows callbacks (called right before {@code onEnable}). */
    public void activate() {
        this.active.set(true);
    }

    /** Rejects all further callbacks (called before {@code onDisable}). */
    public void deactivate() {
        this.active.set(false);
    }

    /** @return whether callbacks are accepted */
    public boolean isActive() {
        return this.active.get();
    }

    /** @return number of callbacks currently executing */
    public int inFlight() {
        return this.inFlight.get();
    }

    /** @return number of callbacks that threw */
    public long failures() {
        return this.failures.get();
    }

    /** @return number of callbacks dropped because the module was inactive */
    public long dropped() {
        return this.dropped.get();
    }

    /**
     * Runs module code if the module is active.
     *
     * @param what description used in error messages
     * @param body module code
     * @return whether the body ran without throwing
     */
    public boolean run(String what, Runnable body) {
        return call(what, () -> {
            body.run();
            return Boolean.TRUE;
        }, Boolean.FALSE);
    }

    /**
     * Calls module code if the module is active.
     *
     * @param what     description used in error messages
     * @param body     module code
     * @param fallback value returned if the module is inactive or the body throws
     * @param <T>      result type
     * @return the result or {@code fallback}
     */
    public <T> T call(String what, Supplier<T> body, T fallback) {
        if (!this.active.get()) {
            this.dropped.incrementAndGet();
            return fallback;
        }
        this.inFlight.incrementAndGet();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(this.classLoader);
        try {
            return body.get();
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            this.failures.incrementAndGet();
            this.logger.log(Level.SEVERE, "Unhandled exception in " + what + " of module '" + this.moduleId + "'", t);
            return fallback;
        } finally {
            thread.setContextClassLoader(previous);
            this.inFlight.decrementAndGet();
        }
    }
}
