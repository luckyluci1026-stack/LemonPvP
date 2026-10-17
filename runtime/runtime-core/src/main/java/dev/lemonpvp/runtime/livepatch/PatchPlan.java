package dev.lemonpvp.runtime.livepatch;

import java.util.List;
import java.util.Map;

/**
 * What applying a patch would do, decided before anything is changed.
 *
 * @param patchId          patch id
 * @param archiveSha256    digest of the archive the plan was made from
 * @param description      description from the manifest
 * @param mechanism        how the classes would be installed
 * @param entries          one entry per class of the patch
 * @param errors           reasons why the patch cannot be applied (empty = applicable)
 * @param warnings         semantic caveats the operator must read
 * @param capabilities     capabilities the patch needs and what the probes found
 * @param registryVersion  {@link LivePatchRegistry#version()} the plan is based on
 */
public record PatchPlan(String patchId, String archiveSha256, String description, Mechanism mechanism, List<Entry> entries,
                        List<String> errors, List<String> warnings, Map<Capability, CapabilityProbe.Result> capabilities, long registryVersion) {

    public PatchPlan {
        entries = List.copyOf(entries);
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
        capabilities = Map.copyOf(capabilities);
    }

    /** What happens with one class. */
    public enum Action {
        /** Existing class is redefined (or retransformed, see {@link PatchPlan#mechanism()}). */
        REDEFINE,
        /** New class is defined in the package of {@link Entry#hostClass()}. */
        DEFINE,
        /** Class is byte-identical to what runs; nothing to do. */
        UNCHANGED,
        /** Class cannot be applied; see {@link Entry#problem()}. */
        REJECT
    }

    /**
     * One class.
     *
     * @param className binary name
     * @param action    planned action
     * @param changes   detected differences to the running version
     * @param runningSha256 digest of the version that runs now ({@code null} for new classes)
     * @param newSha256 digest of the version in the patch
     * @param replaces  id of the active patch whose version of this class is replaced, or {@code null}
     * @param hostClass existing class used to define a new class in its package, or {@code null}
     * @param problem   why the class cannot be applied, or {@code null}
     */
    public record Entry(String className, Action action, List<ClassChange> changes, String runningSha256, String newSha256,
                        String replaces, String hostClass, String problem) {
        public Entry {
            changes = List.copyOf(changes);
        }
    }

    /** @return whether the patch can be applied */
    public boolean applicable() {
        return this.errors.isEmpty() && this.entries.stream().anyMatch(e -> e.action() == Action.REDEFINE || e.action() == Action.DEFINE);
    }

    /** @return classes that would be redefined */
    public List<Entry> redefinitions() {
        return this.entries.stream().filter(e -> e.action() == Action.REDEFINE).toList();
    }

    /** @return classes that would be defined */
    public List<Entry> definitions() {
        return this.entries.stream().filter(e -> e.action() == Action.DEFINE).toList();
    }
}
