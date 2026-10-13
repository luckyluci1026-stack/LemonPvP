package dev.lemonpvp.runtime.descriptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void semanticVersionPrecedenceFollowsSemver() {
        List<String> ordered = List.of("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2",
                "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0", "2.0.0");
        for (int i = 0; i + 1 < ordered.size(); i++) {
            assertTrue(SemanticVersion.parse(ordered.get(i)).compareTo(SemanticVersion.parse(ordered.get(i + 1))) < 0,
                    ordered.get(i) + " < " + ordered.get(i + 1));
        }
        assertTrue(SemanticVersion.parse("1.0.0+build.5").equals(SemanticVersion.parse("1.0.0")));
    }

    @Test
    void invalidVersionsAreRejected() {
        for (String bad : List.of("1", "1.0", "01.0.0", "1.0.0-01", "v1.0.0", "1.0.0.0", "", " ")) {
            assertFalse(SemanticVersion.isValid(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse(null));
    }

    @Test
    void rangesCombineComparators() {
        VersionRange r = VersionRange.parse(">=1.2.0 <2.0.0");
        assertTrue(r.matches("1.2.0"));
        assertTrue(r.matches("1.9.9"));
        assertFalse(r.matches("2.0.0"));
        assertFalse(r.matches("1.1.9"));
        assertTrue(VersionRange.parse("^1.2.3").matches("1.5.0"));
        assertFalse(VersionRange.parse("^1.2.3").matches("2.0.0-alpha"));
        assertFalse(VersionRange.parse("^0.2.3").matches("0.3.0"));
        assertTrue(VersionRange.parse("~1.2.3").matches("1.2.9"));
        assertFalse(VersionRange.parse("~1.2.3").matches("1.3.0"));
        assertTrue(VersionRange.parse("*").matches("0.0.1"));
        assertTrue(VersionRange.parse("=1.0.0").matches("1.0.0"));
        assertTrue(VersionRange.parse("1.0.0, <=1.0.0").matches("1.0.0"));
        assertFalse(VersionRange.parse(">1.0.0").matches("not-a-version"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(">=banana"));
    }

    @Test
    void minecraftPatternsMatchOnSegmentBoundaries() {
        assertTrue(MinecraftVersionMatcher.matches("26.*", "26.1.2"));
        assertTrue(MinecraftVersionMatcher.matches("26.*", "26.3"));
        assertTrue(MinecraftVersionMatcher.matches("26.*", "26"));
        assertFalse(MinecraftVersionMatcher.matches("26.*", "260.1"));
        assertTrue(MinecraftVersionMatcher.matches("1.21.*", "1.21.11"));
        assertFalse(MinecraftVersionMatcher.matches("1.21.1", "1.21.11"));
        assertTrue(MinecraftVersionMatcher.matches("*", "1.21.11"));
        assertTrue(MinecraftVersionMatcher.matchesAny(List.of("1.21.11", "26.2"), "26.2"));
        assertFalse(MinecraftVersionMatcher.isValidPattern("26.*.1"));
        assertFalse(MinecraftVersionMatcher.isValidPattern("latest"));
    }
}
