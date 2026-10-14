package dev.lemonpvp.runtime.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies that a module jar is the file the administrator intended to deploy.
 *
 * <ul>
 *     <li><b>Sidecar check</b> ({@code verify-integrity}): a file {@code <jar>.sha256} in
 *     {@code sha256sum} format must exist and match. Detects corrupt and partially uploaded
 *     files and accidental replacements.</li>
 *     <li><b>Allow-list</b> ({@code trusted-hashes-file}): the digest must be listed.
 *     This pins exactly which builds may run.</li>
 * </ul>
 *
 * <p>Neither check makes malicious code safe. Whoever can write the sidecar or the
 * allow-list can approve any jar; protect those files like the server jar itself.</p>
 */
public final class IntegrityVerifier {

    private final boolean requireSidecar;
    private final Optional<Path> trustedHashesFile;

    /**
     * @param requireSidecar    require a matching {@code .sha256} file
     * @param trustedHashesFile optional allow-list file
     */
    public IntegrityVerifier(boolean requireSidecar, Optional<Path> trustedHashesFile) {
        this.requireSidecar = requireSidecar;
        this.trustedHashesFile = trustedHashesFile;
    }

    /**
     * Result of a verification.
     *
     * @param sha256 digest of the jar
     * @param checks human readable list of the checks that passed
     */
    public record Verified(String sha256, List<String> checks) {
        public Verified {
            checks = List.copyOf(checks);
        }
    }

    /**
     * Hashes and verifies a jar. Performs file I/O; never call on the main thread.
     *
     * @param jar module jar
     * @return the verification result
     * @throws IntegrityException if a check fails
     * @throws IOException        if files cannot be read
     */
    public Verified verify(Path jar) throws IntegrityException, IOException {
        return verifyDigest(jar, Hashing.sha256(jar));
    }

    /**
     * Applies the sidecar and allow-list checks to a digest that the caller computed itself,
     * e.g. over bytes it already read into memory (so the checked bytes are exactly the
     * bytes that are used afterwards). Performs file I/O; never call on the main thread.
     *
     * @param jar    file the digest belongs to (its {@code .sha256} sidecar is read)
     * @param actual SHA-256 of the file content
     * @return the verification result
     * @throws IntegrityException if a check fails
     * @throws IOException        if files cannot be read
     */
    public Verified verifyDigest(Path jar, String actual) throws IntegrityException, IOException {
        List<String> checks = new java.util.ArrayList<>();
        checks.add("sha256=" + actual);
        if (this.requireSidecar) {
            Path sidecar = jar.resolveSibling(jar.getFileName() + ".sha256");
            if (!Files.exists(sidecar)) {
                throw new IntegrityException("missing " + sidecar.getFileName() + " (verify-integrity is enabled)");
            }
            PathGuard.requireRegularFile(sidecar);
            if (Files.size(sidecar) > 4096) {
                throw new IntegrityException(sidecar.getFileName() + " is too large");
            }
            String expected = parseSidecar(Files.readString(sidecar, StandardCharsets.UTF_8), jar.getFileName().toString());
            if (!expected.equals(actual)) {
                throw new IntegrityException("SHA-256 mismatch: " + sidecar.getFileName() + " expects " + expected
                        + " but " + jar.getFileName() + " has " + actual);
            }
            checks.add("sidecar " + sidecar.getFileName() + " matches");
        }
        if (this.trustedHashesFile.isPresent()) {
            Path file = this.trustedHashesFile.get();
            if (!Files.isRegularFile(file)) {
                throw new IntegrityException("trusted-hashes-file " + file.getFileName() + " does not exist");
            }
            if (!loadTrusted(file).contains(actual)) {
                throw new IntegrityException("digest " + actual + " is not listed in " + file.getFileName());
            }
            checks.add("digest listed in " + file.getFileName());
        }
        return new Verified(actual, checks);
    }

    /**
     * Parses {@code sha256sum} output: {@code <hex>} optionally followed by whitespace,
     * an optional {@code *} and the file name.
     *
     * @param content     sidecar file content
     * @param jarFileName expected file name (checked when present)
     * @return the expected digest
     * @throws IntegrityException if the content is malformed or names another file
     */
    static String parseSidecar(String content, String jarFileName) throws IntegrityException {
        String line = content.strip();
        int nl = line.indexOf('\n');
        if (nl >= 0) {
            line = line.substring(0, nl).strip();
        }
        String[] parts = line.split("\\s+", 2);
        String hex = parts[0].toLowerCase(Locale.ROOT);
        if (!Hashing.isSha256(hex)) {
            throw new IntegrityException("sidecar does not start with a SHA-256 hex digest");
        }
        if (parts.length == 2) {
            String named = parts[1].startsWith("*") ? parts[1].substring(1) : parts[1];
            if (!named.equals(jarFileName)) {
                throw new IntegrityException("sidecar refers to '" + PathGuard.sanitize(named) + "' instead of '" + jarFileName + "'");
            }
        }
        return hex;
    }

    private static Set<String> loadTrusted(Path file) throws IOException {
        if (Files.size(file) > 1024 * 1024) {
            throw new IOException("trusted hashes file is larger than 1 MiB");
        }
        Set<String> out = new HashSet<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String hex = line.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
            if (Hashing.isSha256(hex)) {
                out.add(hex);
            }
        }
        return out;
    }

    /** A failed integrity check. */
    public static final class IntegrityException extends Exception {
        public IntegrityException(String message) {
            super(message);
        }
    }
}
