package dev.lemonpvp.runtime.livepatch;

import java.util.regex.Pattern;

/** Validation of live-patch ids and class names used in patches. */
public final class PatchIds {

    /** Patch ids: lower case, digits, '-' and '_', starting with a letter, 2-64 characters. */
    public static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,63}");

    /** Binary class names (ASCII identifiers separated by dots, '$' allowed for nested classes). */
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

    /** File name suffix of patch archives. */
    public static final String SUFFIX = ".lemonpatch";

    /**
     * Class name prefixes that are never patched: the JDK, the live-patch machinery itself
     * (a patch must not change the code that applies or rolls it back), and what that
     * machinery relies on for its checks: integrity, allow-lists, path checks and the audit
     * log ({@code security}) and the parsing of the switches and profiles ({@code config}).
     */
    private static final String[] FORBIDDEN_PREFIXES = {
        "java.", "javax.", "jdk.", "sun.", "com.sun.",
        "dev.lemonpvp.runtime.livepatch.", "dev.lemonpvp.livepatch.",
        "dev.lemonpvp.runtime.security.", "dev.lemonpvp.runtime.config.",
    };

    private PatchIds() {
    }

    /**
     * @param id candidate id
     * @return problem description, or {@code null} if valid
     */
    public static String problem(String id) {
        if (id == null || !PATTERN.matcher(id).matches()) {
            return "patch ids must match " + PATTERN.pattern();
        }
        return null;
    }

    /**
     * @param className binary class name, e.g. {@code org.spigotmc.TicksPerSecondCommand}
     * @return problem description, or {@code null} if the class may be patched
     */
    public static String classProblem(String className) {
        if (className == null || className.length() > 512 || !CLASS_NAME.matcher(className).matches()) {
            return "invalid class name";
        }
        for (String prefix : FORBIDDEN_PREFIXES) {
            if (className.startsWith(prefix)) {
                return "classes in " + prefix + "* cannot be live-patched";
            }
        }
        if (className.endsWith("package-info") || className.equals("module-info")) {
            return "package-info and module-info cannot be live-patched";
        }
        return null;
    }
}
