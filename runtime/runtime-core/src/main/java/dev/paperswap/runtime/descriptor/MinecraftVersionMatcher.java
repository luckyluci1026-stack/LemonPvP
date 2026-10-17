package dev.paperswap.runtime.descriptor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Matches the running Minecraft version against the patterns from
 * {@code minecraft-versions} in a module descriptor.
 *
 * <p>Supported patterns: {@code *} (any), exact versions ({@code 1.21.11}, {@code 26.2})
 * and prefix wildcards on segment boundaries ({@code 26.*} matches {@code 26.1.2} and
 * {@code 26.3}, but not {@code 260.1}).</p>
 */
public final class MinecraftVersionMatcher {

    private static final Pattern VALID_PATTERN = Pattern.compile("\\*|\\d{1,4}(\\.\\d{1,4}){0,3}(\\.\\*)?");

    private MinecraftVersionMatcher() {
    }

    /** @param pattern candidate @return whether the pattern is syntactically valid */
    public static boolean isValidPattern(String pattern) {
        return pattern != null && VALID_PATTERN.matcher(pattern).matches();
    }

    /**
     * @param patterns         descriptor patterns
     * @param minecraftVersion running version, e.g. {@code "26.2"}
     * @return whether any pattern matches
     */
    public static boolean matchesAny(List<String> patterns, String minecraftVersion) {
        for (String p : patterns) {
            if (matches(p, minecraftVersion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param pattern          descriptor pattern
     * @param minecraftVersion running version
     * @return whether the pattern matches
     */
    public static boolean matches(String pattern, String minecraftVersion) {
        if (pattern == null || minecraftVersion == null) {
            return false;
        }
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1); // keeps the trailing dot
            return minecraftVersion.startsWith(prefix) || minecraftVersion.equals(prefix.substring(0, prefix.length() - 1));
        }
        return pattern.equals(minecraftVersion);
    }
}
