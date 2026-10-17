package dev.paperswap.runtime.loader;

import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.security.IntegrityVerifier.IntegrityException;
import dev.paperswap.runtime.security.PathGuard;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Copies a verified module jar into the private staging directory before it is loaded.
 *
 * <p>The class loader always reads the staged copy. Later changes to the file in the
 * module directory therefore cannot alter a running module (no time-of-check/time-of-use
 * gap), and the original can be replaced while the old version is still loaded.</p>
 */
public final class ArtifactStager {

    private final Path stagingDirectory;

    /** @param stagingDirectory private directory for staged jars */
    public ArtifactStager(Path stagingDirectory) {
        this.stagingDirectory = stagingDirectory.toAbsolutePath().normalize();
    }

    /**
     * Copies the jar and verifies that the copy has the expected digest.
     *
     * @param artifact       artifact to stage
     * @param expectedSha256 digest computed during verification
     * @return path of the staged copy
     * @throws IOException        if copying fails
     * @throws IntegrityException if the file changed between verification and staging
     */
    public Path stage(ModuleArtifact artifact, String expectedSha256) throws IOException, IntegrityException {
        Files.createDirectories(this.stagingDirectory);
        String name = artifact.id() + "-" + artifact.descriptor().version() + "-" + expectedSha256.substring(0, 16) + ".jar";
        Path target = PathGuard.resolveInside(this.stagingDirectory, name);
        if (Files.exists(target) && expectedSha256.equals(Hashing.sha256(target))) {
            return target;
        }
        Path tmp = Files.createTempFile(this.stagingDirectory, "stage-", ".tmp");
        try {
            Files.copy(artifact.file(), tmp, StandardCopyOption.REPLACE_EXISTING);
            String actual = Hashing.sha256(tmp);
            if (!actual.equals(expectedSha256)) {
                throw new IntegrityException("module file changed while it was being deployed (expected " + expectedSha256
                        + ", copied " + actual + "); try again once the upload is complete");
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Deletes staged files that are not in use.
     *
     * @param inUse staged files that must be kept
     * @return problems while deleting (files may be locked on some systems)
     */
    public List<String> cleanup(Set<Path> inUse) {
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(this.stagingDirectory)) {
            return problems;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.stagingDirectory)) {
            for (Path p : stream) {
                if (!inUse.contains(p.toAbsolutePath().normalize())) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        problems.add("could not delete " + p.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            problems.add("could not list staging directory: " + e.getMessage());
        }
        return problems;
    }

    /**
     * @param staged staged jar to delete
     * @throws IOException if deletion fails
     */
    public void delete(Path staged) throws IOException {
        PathGuard.requireInside(this.stagingDirectory, staged);
        Files.deleteIfExists(staged);
    }
}
