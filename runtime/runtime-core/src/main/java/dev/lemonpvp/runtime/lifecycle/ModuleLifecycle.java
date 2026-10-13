package dev.lemonpvp.runtime.lifecycle;

import dev.lemonpvp.runtime.api.Capability;
import dev.lemonpvp.runtime.api.ModuleState;
import dev.lemonpvp.runtime.api.RuntimeModule;
import dev.lemonpvp.runtime.loader.ModuleClassLoader;
import dev.lemonpvp.runtime.platform.CallbackGuard;
import dev.lemonpvp.runtime.platform.ModuleHandle;
import dev.lemonpvp.runtime.platform.PlatformBinding;
import dev.lemonpvp.runtime.resources.ResourceTracker;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main-thread lifecycle steps of a single module instance. Every method here must be
 * called on the server main thread; module callbacks run synchronously inside them.
 * Failures never propagate as unchecked exceptions into the server: they are converted
 * into {@link LifecycleException}s after partially registered resources were released.
 */
final class ModuleLifecycle {

    /** Resource category used by platform adapters for commands. */
    static final String CATEGORY_COMMAND = "command";

    private final RuntimeEnvironment env;
    private volatile boolean shutdownMode;

    ModuleLifecycle(RuntimeEnvironment env) {
        this.env = env;
    }

    /**
     * Allows lifecycle steps on any thread. Used only while the server shuts down, when no
     * further ticks run and Paper may stop plugins from a non-main thread.
     */
    void enterShutdownMode() {
        this.shutdownMode = true;
    }

    private void requireMainThread() {
        if (!this.shutdownMode && !this.env.platform().isMainThread()) {
            throw new IllegalStateException("module lifecycle steps must run on the server main thread");
        }
    }

    /**
     * Instantiates the module class and calls {@code onLoad}.
     *
     * @param instance instance in state VALIDATED with a class loader
     * @throws LifecycleException if instantiation or {@code onLoad} fails; the instance is then FAILED
     */
    void instantiate(ModuleInstance instance) throws LifecycleException {
        requireMainThread();
        long start = System.nanoTime();
        ModuleClassLoader loader = instance.classLoader();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            Class<?> mainClass = Class.forName(instance.descriptor().mainClass(), true, loader);
            if (mainClass.getClassLoader() != loader) {
                throw new LifecycleException("main class " + mainClass.getName() + " is not defined by the module jar");
            }
            if (!RuntimeModule.class.isAssignableFrom(mainClass)) {
                throw new LifecycleException("main class " + mainClass.getName() + " does not implement " + RuntimeModule.class.getName());
            }
            if (Modifier.isAbstract(mainClass.getModifiers())) {
                throw new LifecycleException("main class " + mainClass.getName() + " is abstract");
            }
            Constructor<?> ctor = mainClass.getDeclaredConstructor();
            if (!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(mainClass.getModifiers())) {
                throw new LifecycleException("main class and its no-argument constructor must be public");
            }
            RuntimeModule module = (RuntimeModule) ctor.newInstance();
            instance.module(module);
            module.onLoad(instance.context());
            instance.loadMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            warnIfSlow(instance, "onLoad", start);
            instance.transition(ModuleState.LOADED, null);
        } catch (LifecycleException e) {
            fail(instance, "load", e.getMessage());
            throw e;
        } catch (InvocationTargetException e) {
            throw failWith(instance, "load", "constructor threw " + e.getCause(), e.getCause());
        } catch (NoSuchMethodException e) {
            throw failWith(instance, "load", "main class has no public no-argument constructor", null);
        } catch (VirtualMachineError e) {
            fail(instance, "load", e.toString());
            throw e;
        } catch (Throwable t) {
            throw failWith(instance, "load", "loading failed: " + t, t);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Enables an instance (state LOADED or DISABLED).
     *
     * @param instance instance
     * @return detail lines (e.g. slow callback warnings)
     * @throws LifecycleException if {@code onEnable} fails; resources registered so far are released and the instance is FAILED
     */
    List<String> enable(ModuleInstance instance) throws LifecycleException {
        requireMainThread();
        long start = System.nanoTime();
        instance.transition(ModuleState.ENABLING, null);
        ResourceTracker tracker = new ResourceTracker(instance.id());
        CallbackGuard guard = new CallbackGuard(instance.id(), instance.logger(), instance.classLoader());
        instance.tracker(tracker);
        instance.callbacks(guard);
        PlatformBinding binding;
        try {
            binding = this.env.platform().bind(new ModuleHandle(instance.descriptor(), tracker, guard, instance.logger(),
                    instance.classLoader()));
        } catch (RuntimeException e) {
            List<String> problems = tracker.closeAll();
            fail(instance, "enable", "platform binding failed: " + e);
            throw new LifecycleException("platform binding failed for " + instance.descriptor().coordinates() + ": " + e, problems);
        }
        instance.binding(binding);
        guard.activate();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(instance.classLoader());
        Throwable failure = null;
        try {
            instance.module().onEnable(instance.context());
        } catch (VirtualMachineError e) {
            failure = e;
        } catch (Throwable t) {
            failure = t;
        } finally {
            thread.setContextClassLoader(previous);
        }
        if (failure != null) {
            guard.deactivate();
            boolean hadCommands = tracker.countsByCategory().containsKey(CATEGORY_COMMAND);
            List<String> problems = new ArrayList<>(tracker.closeAll());
            problems.addAll(safeClose(binding));
            if (hadCommands) {
                this.env.platform().commandsChanged();
            }
            instance.cleanupProblems(problems);
            this.env.platform().logger().log(Level.SEVERE, "Module " + instance.descriptor().coordinates()
                    + " failed to enable; all resources it registered were released", failure);
            fail(instance, "enable", "onEnable threw " + failure);
            if (!problems.isEmpty()) {
                this.env.health().recommendRestart(instance.id(), "cleanup after failed enable was incomplete: " + problems);
            }
            if (failure instanceof VirtualMachineError vme) {
                throw vme;
            }
            List<String> details = new ArrayList<>();
            details.add("all resources registered during onEnable were released");
            details.addAll(problems);
            throw new LifecycleException(instance.descriptor().coordinates() + ": onEnable threw " + failure, details);
        }
        instance.enableMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        instance.enabledSince(Instant.now());
        instance.cleanupProblems(List.of());
        instance.lastError(null);
        instance.transition(ModuleState.ENABLED, null);
        if (tracker.countsByCategory().containsKey(CATEGORY_COMMAND)) {
            this.env.platform().commandsChanged();
        }
        List<String> details = new ArrayList<>();
        String slow = warnIfSlow(instance, "onEnable", start);
        if (slow != null) {
            details.add(slow);
        }
        if (!instance.descriptor().has(Capability.SERVICES) && !this.env.services().providedBy(instance.id()).isEmpty()) {
            details.add("module provides services without declaring the capability");
        }
        return details;
    }

    /**
     * Disables an enabled instance. Resources are released even if {@code onDisable} throws.
     *
     * @param instance enabled instance
     * @return problems and warnings; if cleanup problems occurred the instance is FAILED, otherwise DISABLED
     */
    List<String> disable(ModuleInstance instance) {
        requireMainThread();
        long start = System.nanoTime();
        instance.transition(ModuleState.DISABLING, null);
        CallbackGuard guard = instance.callbacks();
        ResourceTracker tracker = instance.tracker();
        PlatformBinding binding = instance.binding();
        guard.deactivate();
        boolean hadCommands = tracker.countsByCategory().containsKey(CATEGORY_COMMAND);
        List<String> problems = new ArrayList<>();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(instance.classLoader());
        try {
            instance.module().onDisable(instance.context());
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            problems.add("onDisable threw " + t);
            this.env.platform().logger().log(Level.SEVERE, "Module " + instance.descriptor().coordinates()
                    + " threw in onDisable; releasing its resources anyway", t);
        } finally {
            thread.setContextClassLoader(previous);
        }
        problems.addAll(tracker.closeAll());
        problems.addAll(safeClose(binding));
        if (hadCommands) {
            this.env.platform().commandsChanged();
        }
        instance.disableMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        instance.enabledSince(null);
        List<String> warnings = new ArrayList<>();
        if (guard.inFlight() > 0) {
            warnings.add(guard.inFlight() + " asynchronous callback(s) were still running during disable");
        }
        for (String w : runningAsyncWork(instance)) {
            warnings.add("still running: " + w);
        }
        String slow = warnIfSlow(instance, "disable", start);
        if (slow != null) {
            warnings.add(slow);
        }
        instance.cleanupProblems(problems);
        boolean cleanupFailed = problems.stream().anyMatch(p -> !p.startsWith("onDisable threw"));
        if (cleanupFailed) {
            instance.lastError("cleanup incomplete");
            instance.transition(ModuleState.FAILED, "cleanup incomplete");
            this.env.health().recommendRestart(instance.id(), "resources could not be released: " + problems);
        } else {
            instance.transition(ModuleState.DISABLED, problems.isEmpty() ? null : "onDisable threw");
        }
        List<String> out = new ArrayList<>(problems);
        out.addAll(warnings);
        return out;
    }

    /**
     * Unloads an instance (LOADED, DISABLED or FAILED): closes the class loader and drops
     * all references the runtime holds.
     *
     * @param instance instance
     * @return problems while closing
     * @throws LifecycleException if asynchronous work of the module is still running
     */
    List<String> unload(ModuleInstance instance) throws LifecycleException {
        requireMainThread();
        List<String> running = runningAsyncWork(instance);
        CallbackGuard guard = instance.callbacks();
        if (guard != null && guard.inFlight() > 0) {
            running = new ArrayList<>(running);
            running.add(guard.inFlight() + " callback(s) in flight");
        }
        if (!running.isEmpty()) {
            throw new LifecycleException("module '" + instance.id() + "' still has running asynchronous work; unloading now "
                    + "would break it. Try again later or restart the server.", running);
        }
        List<String> problems = new ArrayList<>();
        ModuleClassLoader loader = instance.classLoader();
        if (loader != null) {
            try {
                loader.close();
            } catch (IOException e) {
                problems.add("closing class loader: " + e.getMessage());
            }
            this.env.leaks().track(instance.id(), instance.descriptor().version(), loader);
        }
        instance.transition(ModuleState.UNLOADED, null);
        instance.releaseReferences();
        return problems;
    }

    private static List<String> runningAsyncWork(ModuleInstance instance) {
        PlatformBinding b = instance.binding();
        if (b == null) {
            return List.of();
        }
        try {
            return b.runningAsyncWork();
        } catch (RuntimeException e) {
            return List.of("unknown (" + e + ")");
        }
    }

    private static List<String> safeClose(PlatformBinding binding) {
        try {
            return binding.close();
        } catch (RuntimeException e) {
            return List.of("platform cleanup threw " + e);
        }
    }

    private String warnIfSlow(ModuleInstance instance, String phase, long startNanos) {
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        long limit = this.env.config().get().operationTimeout().toMillis();
        if (ms > limit) {
            String msg = phase + " of " + instance.id() + " blocked the main thread for " + ms + " ms (operation-timeout " + limit + " ms)";
            this.env.platform().logger().warning(msg);
            return msg;
        }
        return null;
    }

    private void fail(ModuleInstance instance, String phase, String message) {
        instance.lastError(phase + ": " + message);
        if (instance.state() != ModuleState.FAILED && instance.state() != ModuleState.UNLOADED) {
            instance.transition(ModuleState.FAILED, phase + " failed");
        }
    }

    private LifecycleException failWith(ModuleInstance instance, String phase, String message, Throwable cause) {
        if (cause != null) {
            this.env.platform().logger().log(Level.SEVERE, "Module " + instance.descriptor().coordinates() + " failed during "
                    + phase, cause);
        }
        fail(instance, phase, message);
        return new LifecycleException(instance.descriptor().coordinates() + ": " + message);
    }
}
