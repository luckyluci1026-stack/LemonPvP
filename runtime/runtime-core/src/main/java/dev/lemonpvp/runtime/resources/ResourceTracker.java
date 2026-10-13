package dev.lemonpvp.runtime.resources;

import dev.lemonpvp.runtime.api.ResourceScope;
import dev.lemonpvp.runtime.api.TrackedResource;
import dev.lemonpvp.runtime.security.PathGuard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Per-module resource registry. One tracker exists per enable cycle of a module instance.
 *
 * <p>Resources are released in reverse registration order (LIFO), so a resource that
 * depends on an earlier one is released first. Failures during release are collected
 * rather than aborting the cleanup, so one broken resource cannot prevent the others from
 * being released. Thread-safe.</p>
 */
public final class ResourceTracker implements ResourceScope {

    /** Category used for resources registered without an explicit category. */
    public static final String CATEGORY_CUSTOM = "custom";

    private final String owner;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private boolean closed;

    /** @param owner module id used in messages */
    public ResourceTracker(String owner) {
        this.owner = owner;
    }

    @Override
    public <T extends AutoCloseable> T track(String description, T resource) {
        Objects.requireNonNull(resource, "resource");
        register(CATEGORY_CUSTOM, description, () -> {
            try {
                resource.close();
            } catch (Exception e) {
                throw new CleanupException(e);
            }
        });
        return resource;
    }

    @Override
    public TrackedResource onCleanup(String description, Runnable cleanup) {
        return register(CATEGORY_CUSTOM, description, cleanup);
    }

    /**
     * Registers a categorized cleanup action (used by platform adapters).
     *
     * @param category    category for diagnostics, e.g. {@code listener}, {@code task}
     * @param description human readable description
     * @param cleanup     cleanup action
     * @return handle for early release
     * @throws IllegalStateException if the tracker was closed; {@code cleanup} is run immediately
     */
    public TrackedResource register(String category, String description, Runnable cleanup) {
        Objects.requireNonNull(cleanup, "cleanup");
        Entry entry = new Entry(category, PathGuard.sanitize(description), cleanup);
        synchronized (this) {
            if (!this.closed) {
                this.entries.push(entry);
                return entry;
            }
        }
        // Late registration after disable: release immediately so nothing leaks.
        entry.release();
        throw new IllegalStateException("module '" + this.owner + "' is not enabled; resource '" + entry.description
                + "' was released immediately");
    }

    /** @return whether new resources can still be registered */
    public synchronized boolean isOpen() {
        return !this.closed;
    }

    @Override
    public synchronized int size() {
        return this.entries.size();
    }

    /** @return number of live resources per category */
    public synchronized Map<String, Integer> countsByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Entry e : this.entries) {
            counts.merge(e.category, 1, Integer::sum);
        }
        return counts;
    }

    /** @return descriptions of live resources, most recent first */
    public synchronized List<String> describe() {
        List<String> out = new ArrayList<>();
        for (Entry e : this.entries) {
            out.add(e.category + ": " + e.description);
        }
        return out;
    }

    /**
     * Closes the tracker and releases all resources in LIFO order.
     *
     * @return problems that occurred; empty if everything was released cleanly
     */
    public List<String> closeAll() {
        List<Entry> toRelease;
        synchronized (this) {
            this.closed = true;
            toRelease = new ArrayList<>(this.entries);
            this.entries.clear();
        }
        List<String> problems = new ArrayList<>();
        for (Entry e : toRelease) {
            String problem = e.release();
            if (problem != null) {
                problems.add(problem);
            }
        }
        return problems;
    }

    private synchronized void remove(Entry entry) {
        Iterator<Entry> it = this.entries.iterator();
        while (it.hasNext()) {
            if (it.next() == entry) {
                it.remove();
                return;
            }
        }
    }

    private final class Entry implements TrackedResource {
        private final String category;
        private final String description;
        private final Runnable cleanup;
        private boolean released;

        private Entry(String category, String description, Runnable cleanup) {
            this.category = category;
            this.description = description;
            this.cleanup = cleanup;
        }

        @Override
        public String description() {
            return this.description;
        }

        @Override
        public synchronized boolean isReleased() {
            return this.released;
        }

        @Override
        public void close() {
            remove(this);
            release();
        }

        /** @return problem description or {@code null} */
        private String release() {
            synchronized (this) {
                if (this.released) {
                    return null;
                }
                this.released = true;
            }
            try {
                this.cleanup.run();
                return null;
            } catch (RuntimeException | LinkageError e) {
                Throwable cause = e instanceof CleanupException ce ? ce.getCause() : e;
                return this.category + " '" + this.description + "': " + cause;
            }
        }
    }

    private static final class CleanupException extends RuntimeException {
        private CleanupException(Throwable cause) {
            super(cause);
        }
    }
}
