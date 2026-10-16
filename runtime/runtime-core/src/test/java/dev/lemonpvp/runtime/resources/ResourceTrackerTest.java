package dev.lemonpvp.runtime.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.runtime.api.TrackedResource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceTrackerTest {

    @Test
    void releasesInReverseOrderAndCollectsFailures() {
        List<String> order = new ArrayList<>();
        ResourceTracker t = new ResourceTracker("m");
        t.register("listener", "first", () -> order.add("first"));
        t.register("task", "broken", () -> {
            throw new IllegalStateException("cannot cancel");
        });
        t.track("closeable", () -> order.add("closeable"));
        assertEquals(3, t.size());
        List<String> problems = t.closeAll();
        assertEquals(List.of("closeable", "first"), order, "LIFO and the broken one does not stop the rest");
        assertEquals(1, problems.size());
        assertTrue(problems.getFirst().contains("cannot cancel"));
        assertEquals(0, t.size());
    }

    @Test
    void lateRegistrationIsReleasedImmediately() {
        ResourceTracker t = new ResourceTracker("m");
        t.closeAll();
        List<String> released = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> t.register("task", "late", () -> released.add("late")));
        assertEquals(List.of("late"), released);
        assertFalse(t.isOpen());
    }

    @Test
    void earlyReleaseRemovesTheEntryOnce() {
        List<String> released = new ArrayList<>();
        ResourceTracker t = new ResourceTracker("m");
        TrackedResource r = t.onCleanup("x", () -> released.add("x"));
        r.close();
        r.close();
        assertTrue(r.isReleased());
        assertEquals(0, t.size());
        t.closeAll();
        assertEquals(List.of("x"), released);
    }

    @Test
    void releaseOrderStaysLifoAfterEarlyReleases() {
        List<String> order = new ArrayList<>();
        ResourceTracker t = new ResourceTracker("m");
        t.register("a", "a", () -> order.add("a"));
        TrackedResource b = t.register("b", "b", () -> order.add("b"));
        t.register("c", "c", () -> order.add("c"));
        t.register("d", "d", () -> order.add("d"));
        b.close();
        assertEquals(List.of("d: d", "c: c", "a: a"), t.describe(), "most recent first");
        t.closeAll();
        assertEquals(List.of("b", "d", "c", "a"), order);
    }

    @Test
    void releasingManyResourcesOneByOneStaysFast() {
        // Measured with the former ArrayDeque (linear search per release): 50,000 handles took
        // 1.3 s; 200,000 would take tens of seconds on the server main thread.
        ResourceTracker t = new ResourceTracker("m");
        List<TrackedResource> handles = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            handles.add(t.register("listener", "l" + i, () -> { }));
        }
        assertTimeout(Duration.ofSeconds(5), () -> handles.forEach(TrackedResource::close));
        assertEquals(0, t.size());
    }
}
