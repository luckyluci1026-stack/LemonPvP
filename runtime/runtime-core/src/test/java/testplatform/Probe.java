package testplatform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records what test modules did, readable from the test. */
public final class Probe {

    private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    private Probe() {
    }

    public static void record(String event) {
        EVENTS.add(event);
    }

    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    public static void clear() {
        EVENTS.clear();
    }
}
