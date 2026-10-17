package dev.paperswap.runtime.metrics;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observes class loaders of unloaded module instances.
 *
 * <p>Closing a class loader does not unload its classes. The JVM can only unload them
 * after every reference to the loader, its classes and their instances is gone (static
 * fields held elsewhere, running threads, thread locals, registered callbacks...). This
 * class keeps a {@link WeakReference} per unloaded instance and reports which ones are
 * still reachable. It never triggers garbage collection itself; a loader reported as
 * "still reachable" shortly after an unload may simply not have been collected yet.</p>
 */
public final class LeakDetector {

    private static final int MAX_ENTRIES = 256;

    /**
     * Status of one unloaded instance.
     *
     * @param moduleId   module id
     * @param version    module version
     * @param unloadedAt time of the unload
     * @param collected  whether the class loader has been garbage collected
     * @param liveThreads names of live threads that still reference the class loader
     */
    public record Entry(String moduleId, String version, Instant unloadedAt, boolean collected, List<String> liveThreads) {
    }

    private record Tracked(String moduleId, String version, Instant unloadedAt, WeakReference<ClassLoader> loader) {
    }

    private final List<Tracked> tracked = new CopyOnWriteArrayList<>();

    /**
     * @param moduleId module id
     * @param version  module version
     * @param loader   class loader that was just closed
     */
    public void track(String moduleId, String version, ClassLoader loader) {
        if (this.tracked.size() >= MAX_ENTRIES) {
            this.tracked.removeIf(t -> t.loader().get() == null);
            if (this.tracked.size() >= MAX_ENTRIES) {
                this.tracked.removeFirst();
            }
        }
        this.tracked.add(new Tracked(moduleId, version, Instant.now(), new WeakReference<>(loader)));
    }

    /**
     * Builds a report. Inspects live threads; call only on demand (diagnostics command).
     *
     * @param moduleId module to report, or {@code null} for all
     * @return report entries, oldest first
     */
    public List<Entry> report(String moduleId) {
        List<Entry> out = new ArrayList<>();
        Iterator<Tracked> it = this.tracked.iterator();
        while (it.hasNext()) {
            Tracked t = it.next();
            if (moduleId != null && !moduleId.equals(t.moduleId())) {
                continue;
            }
            ClassLoader loader = t.loader().get();
            out.add(new Entry(t.moduleId(), t.version(), t.unloadedAt(), loader == null,
                    loader == null ? List.of() : threadsReferencing(loader)));
        }
        return out;
    }

    /**
     * @param loader class loader
     * @return names of live threads whose context class loader is {@code loader} or whose
     * thread class was defined by it
     */
    public static List<String> threadsReferencing(ClassLoader loader) {
        List<String> names = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getContextClassLoader() == loader || thread.getClass().getClassLoader() == loader) {
                names.add(thread.getName());
            }
        }
        return names;
    }
}
