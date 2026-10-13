package dev.lemonpvp.runtime.metrics;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Simple counters for administrative operations. Cheap enough to always be on. */
public final class RuntimeMetrics {

    private final Map<String, AtomicLong> succeeded = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failed = new ConcurrentHashMap<>();

    /** @param action operation name @param success outcome */
    public void record(String action, boolean success) {
        (success ? this.succeeded : this.failed).computeIfAbsent(action, k -> new AtomicLong()).incrementAndGet();
    }

    /** @return action -> "succeeded/failed" counts, sorted by action */
    public Map<String, String> summary() {
        Map<String, String> out = new TreeMap<>();
        for (String k : this.succeeded.keySet()) {
            out.put(k, "");
        }
        for (String k : this.failed.keySet()) {
            out.put(k, "");
        }
        out.replaceAll((k, v) -> count(this.succeeded, k) + " ok / " + count(this.failed, k) + " failed");
        return out;
    }

    private static long count(Map<String, AtomicLong> map, String key) {
        AtomicLong v = map.get(key);
        return v == null ? 0 : v.get();
    }
}
