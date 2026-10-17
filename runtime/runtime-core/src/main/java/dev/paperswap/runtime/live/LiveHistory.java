package dev.paperswap.runtime.live;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Recent live-update actions of this JVM (modules, plugins, core patches) for
 * {@code /fork live history}. JVM-global so that Paper's {@code /reload} does not lose it;
 * bounded to the last {@value #CAPACITY} entries. The complete, persistent record is the
 * audit log ({@code paperswap/logs/audit.log}).
 */
public final class LiveHistory {

    /** The history of this JVM. */
    public static final LiveHistory INSTANCE = new LiveHistory();

    /** Number of entries kept in memory. */
    public static final int CAPACITY = 200;

    /** What an entry is about. */
    public enum Kind {
        /** PaperSwap runtime module. */
        MODULE,
        /** Paper/Bukkit plugin. */
        PLUGIN,
        /** Core live patch (class redefinition). */
        PATCH
    }

    /**
     * One action.
     *
     * @param time    when it finished
     * @param kind    area
     * @param name    module id, plugin name or patch id
     * @param action  e.g. {@code enable}, {@code update}, {@code apply}, {@code rollback}
     * @param actor   who triggered it
     * @param success outcome
     * @param summary one line result
     */
    public record Entry(Instant time, Kind kind, String name, String action, String actor, boolean success, String summary) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    LiveHistory() {
    }

    /**
     * Records an action.
     *
     * @param kind    area
     * @param name    subject
     * @param action  action
     * @param actor   who
     * @param success outcome
     * @param summary result line
     */
    public synchronized void record(Kind kind, String name, String action, String actor, boolean success, String summary) {
        this.entries.addLast(new Entry(Instant.now(), kind, name, action, actor, success, summary));
        while (this.entries.size() > CAPACITY) {
            this.entries.removeFirst();
        }
    }

    /**
     * @param limit maximum number of entries
     * @return newest entries first
     */
    public synchronized List<Entry> latest(int limit) {
        List<Entry> out = new ArrayList<>(Math.min(limit, this.entries.size()));
        var it = this.entries.descendingIterator();
        while (it.hasNext() && out.size() < limit) {
            out.add(it.next());
        }
        return out;
    }

    /** Test hook. */
    synchronized void clearForTests() {
        this.entries.clear();
    }
}
