package dev.paperswap.runtime.descriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Version constraint for module dependencies. A range is a list of comparators separated
 * by whitespace or commas that must <em>all</em> match:
 * <ul>
 *     <li>{@code *} - any version</li>
 *     <li>{@code 1.2.3} or {@code =1.2.3} - exactly this version</li>
 *     <li>{@code >1.2.3}, {@code >=1.2.3}, {@code <2.0.0}, {@code <=2.0.0}</li>
 *     <li>{@code ^1.2.3} - {@code >=1.2.3 <2.0.0} (for {@code 0.x}: {@code <0.(x+1).0})</li>
 *     <li>{@code ~1.2.3} - {@code >=1.2.3 <1.3.0}</li>
 * </ul>
 */
public final class VersionRange {

    private final String text;
    private final List<Predicate<SemanticVersion>> comparators;

    private VersionRange(String text, List<Predicate<SemanticVersion>> comparators) {
        this.text = text;
        this.comparators = comparators;
    }

    /**
     * @param text range text
     * @return the parsed range
     * @throws IllegalArgumentException if the range is invalid
     */
    public static VersionRange parse(String text) {
        if (text == null || text.isBlank()) {
            text = "*";
        }
        if (text.length() > 128) {
            throw new IllegalArgumentException("version range too long");
        }
        List<Predicate<SemanticVersion>> list = new ArrayList<>();
        for (String token : text.trim().split("[\\s,]+")) {
            list.add(comparator(token));
        }
        return new VersionRange(text.trim(), List.copyOf(list));
    }

    private static Predicate<SemanticVersion> comparator(String token) {
        if (token.equals("*")) {
            return v -> true;
        }
        if (token.startsWith(">=")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(2));
            return v -> v.compareTo(b) >= 0;
        }
        if (token.startsWith("<=")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(2));
            return v -> v.compareTo(b) <= 0;
        }
        if (token.startsWith(">")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(1));
            return v -> v.compareTo(b) > 0;
        }
        if (token.startsWith("<")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(1));
            return v -> v.compareTo(b) < 0;
        }
        if (token.startsWith("^")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(1));
            SemanticVersion upper = b.major() > 0
                    ? SemanticVersion.parse((b.major() + 1) + ".0.0-0")
                    : SemanticVersion.parse("0." + (b.minor() + 1) + ".0-0");
            return v -> v.compareTo(b) >= 0 && v.compareTo(upper) < 0;
        }
        if (token.startsWith("~")) {
            SemanticVersion b = SemanticVersion.parse(token.substring(1));
            SemanticVersion upper = SemanticVersion.parse(b.major() + "." + (b.minor() + 1) + ".0-0");
            return v -> v.compareTo(b) >= 0 && v.compareTo(upper) < 0;
        }
        SemanticVersion exact = SemanticVersion.parse(token.startsWith("=") ? token.substring(1) : token);
        return v -> v.compareTo(exact) == 0;
    }

    /**
     * @param version version to test
     * @return whether the version satisfies every comparator
     */
    public boolean matches(SemanticVersion version) {
        for (Predicate<SemanticVersion> p : this.comparators) {
            if (!p.test(version)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param version version text
     * @return whether the version is valid and satisfies the range
     */
    public boolean matches(String version) {
        return SemanticVersion.isValid(version) && matches(SemanticVersion.parse(version));
    }

    @Override
    public String toString() {
        return this.text;
    }
}
