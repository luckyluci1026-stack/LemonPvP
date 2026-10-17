package dev.paperswap.runtime.descriptor;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Validation rules for module ids. */
public final class ModuleIds {

    /** 2-48 characters, lower case letters, digits, '-' and '_', starting with a letter. */
    public static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,47}");

    /**
     * Ids that would be confusing as command namespaces or collide with well-known server
     * components.
     */
    public static final Set<String> RESERVED = Set.of(
            "minecraft", "bukkit", "spigot", "paper", "papermc", "craftbukkit", "paperswap", "runtime", "fork");

    private ModuleIds() {
    }

    /**
     * @param id candidate id
     * @return whether the id is syntactically valid and not reserved
     */
    public static boolean isValid(String id) {
        return id != null && PATTERN.matcher(id).matches() && !RESERVED.contains(id);
    }

    /**
     * @param id candidate id
     * @return a human readable reason why the id is invalid, or {@code null} if it is valid
     */
    public static String problem(String id) {
        if (id == null || id.isEmpty()) {
            return "module id is missing";
        }
        if (!PATTERN.matcher(id).matches()) {
            return "module id must match " + PATTERN.pattern() + " (lower case, 2-48 characters)";
        }
        if (RESERVED.contains(id.toLowerCase(Locale.ROOT))) {
            return "module id '" + id + "' is reserved";
        }
        return null;
    }
}
