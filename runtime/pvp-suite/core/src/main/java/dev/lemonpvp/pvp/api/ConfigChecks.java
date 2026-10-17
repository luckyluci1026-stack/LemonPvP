package dev.lemonpvp.pvp.api;

import dev.lemonpvp.runtime.api.ConfigSnapshot;
import java.util.List;
import java.util.Optional;

/** Checks for config.yml values, shared by the modules of the suite (used in validateConfig). */
public final class ConfigChecks {

    private ConfigChecks() {
    }

    /** Adds a problem if {@code key} is present but not a whole number from min to max. */
    public static void wholeNumber(ConfigSnapshot c, String key, int min, int max, List<String> problems) {
        Optional<Object> v = c.get(key);
        if (v.isPresent() && !(v.get() instanceof Integer i && i >= min && i <= max)) {
            problems.add(key + " must be a whole number from " + min + " to " + max);
        }
    }

    /** Adds a problem if {@code key} is present but not true/false. */
    public static void bool(ConfigSnapshot c, String key, List<String> problems) {
        if (c.get(key).filter(v -> !(v instanceof Boolean)).isPresent()) {
            problems.add(key + " must be true or false");
        }
    }

    /** Adds a problem if {@code key} is present but not a text of 1 to 256 characters. */
    public static void text(ConfigSnapshot c, String key, List<String> problems) {
        if (c.get(key).filter(v -> !(v instanceof String s && !s.isBlank() && s.length() <= 256)).isPresent()) {
            problems.add(key + " must be a text of 1 to 256 characters");
        }
    }
}
