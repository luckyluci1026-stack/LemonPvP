package dev.paperswap.runtime.loader;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Result of scanning the module directory. Scanning never activates anything.
 *
 * @param artifacts valid, unambiguous module jars
 * @param problems  files that were rejected and why
 * @param scannedAt time of the scan
 */
public record ScanReport(List<ModuleArtifact> artifacts, List<Problem> problems, Instant scannedAt) {
    public ScanReport {
        artifacts = List.copyOf(artifacts);
        problems = List.copyOf(problems);
    }

    /**
     * @param id module id
     * @return the artifact with this id
     */
    public Optional<ModuleArtifact> find(String id) {
        return this.artifacts.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    /**
     * @param id module id
     * @return problems concerning this module id
     */
    public List<Problem> problemsFor(String id) {
        return this.problems.stream().filter(p -> id.equals(p.moduleId())).toList();
    }

    /**
     * A rejected file.
     *
     * @param file     file name (sanitized)
     * @param moduleId module id if the descriptor could be read, otherwise {@code null}
     * @param message  reason
     */
    public record Problem(String file, String moduleId, String message) {
    }
}
