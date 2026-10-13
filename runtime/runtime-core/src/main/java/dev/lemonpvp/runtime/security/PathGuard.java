package dev.lemonpvp.runtime.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Defensive path handling. Every path the runtime derives from external input (file names
 * in the module directory, resource names from a descriptor, configuration values) goes
 * through this class.
 */
public final class PathGuard {

    /** Plain file names: letters, digits, dot, dash, underscore; must not start with a dot. */
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,127}");
    /** Relative resource paths: safe segments separated by '/'. */
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,127}");

    private PathGuard() {
    }

    /**
     * @param name file name without directories
     * @return whether the name is a safe plain file name
     */
    public static boolean isSafeFileName(String name) {
        return name != null && SAFE_FILE_NAME.matcher(name).matches() && !name.contains("..");
    }

    /**
     * Validates a relative path such as {@code "lang/en.yml"} taken from untrusted input.
     *
     * @param relative relative path using {@code /} as separator
     * @return whether every segment is safe and the path cannot escape its base
     */
    public static boolean isSafeRelativePath(String relative) {
        if (relative == null || relative.isEmpty() || relative.length() > 512
                || relative.startsWith("/") || relative.contains("\\") || relative.contains(":")) {
            return false;
        }
        for (String segment : relative.split("/", -1)) {
            if (!SAFE_SEGMENT.matcher(segment).matches() || segment.equals("..") || segment.contains("..")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves {@code relative} against {@code base} and guarantees the result stays inside
     * {@code base}, also after symbolic links of existing path elements are resolved.
     *
     * @param base     trusted base directory
     * @param relative untrusted relative path
     * @return normalized absolute path inside {@code base}
     * @throws SecurityException if the path is unsafe or escapes the base directory
     */
    public static Path resolveInside(Path base, String relative) {
        if (!isSafeRelativePath(relative)) {
            throw new SecurityException("unsafe path: " + sanitize(relative));
        }
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(relative).normalize();
        requireInside(normalizedBase, resolved);
        return resolved;
    }

    /**
     * @param base      trusted base directory
     * @param candidate candidate path
     * @throws SecurityException if {@code candidate} is not located inside {@code base}
     */
    public static void requireInside(Path base, Path candidate) {
        Path b = base.toAbsolutePath().normalize();
        Path c = candidate.toAbsolutePath().normalize();
        if (!c.startsWith(b) || c.equals(b)) {
            throw new SecurityException("path escapes its base directory: " + sanitize(c.toString()));
        }
        try {
            // Re-check with symbolic links resolved for the parts that exist already.
            if (!resolveExisting(c).startsWith(resolveExisting(b))) {
                throw new SecurityException("path escapes its base directory via a link: " + sanitize(c.toString()));
            }
        } catch (IOException e) {
            throw new SecurityException("cannot verify path " + sanitize(c.toString()) + ": " + e.getMessage(), e);
        }
    }

    /**
     * Resolves symbolic links of the longest existing prefix of {@code path} and appends the
     * not yet existing remainder unchanged.
     */
    private static Path resolveExisting(Path path) throws IOException {
        Path existing = path;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return path;
        }
        return existing.toRealPath().resolve(existing.relativize(path)).normalize();
    }

    /**
     * @param file file to check
     * @throws SecurityException if the file is a symbolic link or not a regular file
     */
    public static void requireRegularFile(Path file) {
        if (Files.isSymbolicLink(file)) {
            throw new SecurityException("symbolic links are not accepted: " + sanitize(file.getFileName().toString()));
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new SecurityException("not a regular file: " + sanitize(file.getFileName().toString()));
        }
    }

    /**
     * Makes untrusted text safe for single-line log and audit output.
     *
     * @param text untrusted text
     * @return text with control characters replaced and length limited
     */
    public static String sanitize(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(Math.min(text.length(), 200));
        for (int i = 0; i < text.length() && sb.length() < 200; i++) {
            char ch = text.charAt(i);
            sb.append(Character.isISOControl(ch) ? '?' : ch);
        }
        if (text.length() > 200) {
            sb.append("...");
        }
        return sb.toString();
    }
}
