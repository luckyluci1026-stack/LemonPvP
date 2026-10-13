package dev.lemonpvp.runtime.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityTest {

    @TempDir
    Path dir;

    @Test
    void relativePathsCannotEscape() {
        for (String bad : List.of("../x", "a/../../x", "/abs", "a\\b", "C:/x", "a//b", "", ".hidden", "a/./b", "..")) {
            assertFalse(PathGuard.isSafeRelativePath(bad), bad);
            assertThrows(SecurityException.class, () -> PathGuard.resolveInside(this.dir, bad), bad);
        }
        assertTrue(PathGuard.isSafeRelativePath("lang/en_us.yml"));
        assertEquals(this.dir.resolve("lang/en.yml").toAbsolutePath(), PathGuard.resolveInside(this.dir, "lang/en.yml"));
    }

    @Test
    void symlinkedDirectoriesCannotBeUsedToEscape() throws Exception {
        Path outside = Files.createDirectories(this.dir.resolve("outside"));
        Path base = Files.createDirectories(this.dir.resolve("base"));
        Files.createSymbolicLink(base.resolve("link"), outside);
        assertThrows(SecurityException.class, () -> PathGuard.resolveInside(base, "link/file.yml"));
        assertEquals(base.resolve("new/file.yml").toAbsolutePath(), PathGuard.resolveInside(base, "new/file.yml"));
    }

    @Test
    void fileNamesAreRestricted() {
        assertTrue(PathGuard.isSafeFileName("my-module_1.2.jar"));
        for (String bad : List.of("../a.jar", "a/b.jar", ".jar", "a..jar", "a b.jar", "a\u0000.jar")) {
            assertFalse(PathGuard.isSafeFileName(bad), bad);
        }
        assertEquals("a?b", PathGuard.sanitize("a\nb"));
    }

    @Test
    void sidecarAndAllowListAreEnforced() throws Exception {
        Path jar = this.dir.resolve("m.jar");
        Files.writeString(jar, "jar-content");
        String sha = Hashing.sha256(jar);
        IntegrityVerifier strict = new IntegrityVerifier(true, Optional.empty());
        assertThrows(IntegrityVerifier.IntegrityException.class, () -> strict.verify(jar), "missing sidecar");

        Files.writeString(this.dir.resolve("m.jar.sha256"), sha + "  other.jar\n");
        assertThrows(IntegrityVerifier.IntegrityException.class, () -> strict.verify(jar), "wrong file name");
        Files.writeString(this.dir.resolve("m.jar.sha256"), sha.toUpperCase() + " *m.jar\n");
        assertEquals(sha, strict.verify(jar).sha256(), "binary-mode sha256sum output is accepted");

        Path allow = this.dir.resolve("trusted.txt");
        Files.writeString(allow, "# comment\n" + "a".repeat(64) + "\n");
        IntegrityVerifier pinned = new IntegrityVerifier(false, Optional.of(allow));
        assertThrows(IntegrityVerifier.IntegrityException.class, () -> pinned.verify(jar));
        Files.writeString(allow, sha + " m 1.0.0\n");
        assertEquals(sha, pinned.verify(jar).sha256());
    }

    @Test
    void auditLogCannotBeForgedWithLineBreaks() throws Exception {
        Path log = this.dir.resolve("audit.log");
        AuditLog audit = new AuditLog(log, Runnable::run, java.util.logging.Logger.getAnonymousLogger());
        audit.record("evil\n2026 | admin | enable", "enable", "x|y", true, "details\r\nforged");
        List<String> lines = Files.readAllLines(log);
        assertEquals(1, lines.size(), lines.toString());
        assertEquals(6, lines.getFirst().split(" \\| ").length, lines.getFirst());
    }
}
