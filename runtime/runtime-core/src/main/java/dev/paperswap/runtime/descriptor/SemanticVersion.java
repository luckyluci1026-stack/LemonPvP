package dev.paperswap.runtime.descriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic version ({@code MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]}) following the
 * precedence rules of semver 2.0.0. Build metadata is ignored for comparison.
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {

    private static final Pattern PATTERN = Pattern.compile(
            "(0|[1-9]\\d{0,8})\\.(0|[1-9]\\d{0,8})\\.(0|[1-9]\\d{0,8})"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?");

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> preRelease;
    private final String text;

    private SemanticVersion(int major, int minor, int patch, List<String> preRelease, String text) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.text = text;
    }

    /**
     * @param text version text
     * @return the parsed version
     * @throws IllegalArgumentException if the text is not a valid semantic version
     */
    public static SemanticVersion parse(String text) {
        if (text == null || text.length() > 64) {
            throw new IllegalArgumentException("invalid semantic version: " + text);
        }
        Matcher m = PATTERN.matcher(text.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("invalid semantic version: " + text);
        }
        List<String> pre = m.group(4) == null ? List.of() : List.of(m.group(4).split("\\."));
        for (String id : pre) {
            if (id.length() > 1 && id.chars().allMatch(Character::isDigit) && id.startsWith("0")) {
                throw new IllegalArgumentException("numeric pre-release identifiers must not have leading zeros: " + text);
            }
        }
        return new SemanticVersion(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), pre, text.trim());
    }

    /** @param text version text @return whether {@link #parse(String)} would succeed */
    public static boolean isValid(String text) {
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public int patch() {
        return this.patch;
    }

    /** @return whether this is a pre-release version */
    public boolean isPreRelease() {
        return !this.preRelease.isEmpty();
    }

    @Override
    public int compareTo(SemanticVersion o) {
        int c = Integer.compare(this.major, o.major);
        if (c == 0) {
            c = Integer.compare(this.minor, o.minor);
        }
        if (c == 0) {
            c = Integer.compare(this.patch, o.patch);
        }
        if (c != 0) {
            return c;
        }
        if (this.preRelease.isEmpty() || o.preRelease.isEmpty()) {
            // A version without pre-release has higher precedence.
            return Boolean.compare(this.preRelease.isEmpty(), o.preRelease.isEmpty());
        }
        int n = Math.min(this.preRelease.size(), o.preRelease.size());
        for (int i = 0; i < n; i++) {
            c = comparePreReleaseId(this.preRelease.get(i), o.preRelease.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(this.preRelease.size(), o.preRelease.size());
    }

    private static int comparePreReleaseId(String a, String b) {
        boolean an = a.chars().allMatch(Character::isDigit);
        boolean bn = b.chars().allMatch(Character::isDigit);
        if (an && bn) {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        }
        if (an != bn) {
            return an ? -1 : 1;
        }
        return a.compareTo(b);
    }

    /** @return core version {@code MAJOR.MINOR.PATCH} without pre-release */
    public SemanticVersion withoutPreRelease() {
        return new SemanticVersion(this.major, this.minor, this.patch, List.of(),
                this.major + "." + this.minor + "." + this.patch);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SemanticVersion v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, new ArrayList<>(this.preRelease));
    }

    @Override
    public String toString() {
        return this.text;
    }
}
