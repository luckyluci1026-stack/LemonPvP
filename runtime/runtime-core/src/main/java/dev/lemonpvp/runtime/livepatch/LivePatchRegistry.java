package dev.lemonpvp.runtime.livepatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What this JVM currently runs, as far as Lemon changed it. JVM-global on purpose: Paper's
 * {@code /reload} restarts the Lemon runtime but the JVM keeps every redefined class, so
 * the record of those redefinitions must survive the runtime instance.
 *
 * <p>Live patches do not survive a JVM restart: after a restart the server runs the code
 * of its jar again. The registry starts empty in every JVM.</p>
 */
public final class LivePatchRegistry {

    /** The registry of this JVM. */
    public static final LivePatchRegistry INSTANCE = new LivePatchRegistry();

    /** State of a patch in this JVM. */
    public enum Status {
        /** Redefinition succeeded and was not rolled back. */
        ACTIVE,
        /** Rolled back to the previous bytecode. */
        ROLLED_BACK,
        /** The JVM rejected the redefinition; nothing was changed. */
        FAILED
    }

    /**
     * A class version installed by Lemon.
     *
     * @param bytes       class file bytes
     * @param sha256      digest of {@code bytes}
     * @param installedBy patch id, or {@code null} for the original version from the jar
     */
    public record ClassVersion(byte[] bytes, String sha256, String installedBy) {
    }

    /**
     * One redefined class of a patch.
     *
     * @param name      binary name
     * @param previous  version before the patch
     * @param installed version installed by the patch
     */
    public record ClassRecord(String name, ClassVersion previous, ClassVersion installed) {
    }

    /**
     * One apply attempt.
     *
     * @param id             patch id
     * @param archiveSha256  digest of the archive
     * @param actor          who applied it
     * @param appliedAt      time of the attempt
     * @param status         current status
     * @param detail         result description
     * @param classes        redefined classes (empty for failures)
     * @param definedClasses new classes defined by the patch (they stay until restart)
     * @param backupDirectory where the previous class files were saved
     * @param changedAt      time of the last status change
     */
    public record PatchRecord(String id, String archiveSha256, String actor, Instant appliedAt, Status status, String detail,
                              List<ClassRecord> classes, List<String> definedClasses, String backupDirectory, Instant changedAt) {
        public PatchRecord {
            classes = List.copyOf(classes);
            definedClasses = List.copyOf(definedClasses);
        }

        PatchRecord with(Status newStatus, String newDetail) {
            return new PatchRecord(this.id, this.archiveSha256, this.actor, this.appliedAt, newStatus, newDetail, this.classes,
                    this.definedClasses, this.backupDirectory, Instant.now());
        }
    }

    private final Map<String, ClassVersion> current = new HashMap<>();
    private final Map<String, String> originalSha = new HashMap<>();
    private final List<PatchRecord> records = new ArrayList<>();
    private long version;

    LivePatchRegistry() {
    }

    /**
     * @param className binary name
     * @return the version Lemon installed, if the class is currently patched
     */
    public synchronized Optional<ClassVersion> current(String className) {
        ClassVersion v = this.current.get(className);
        return v == null || v.installedBy() == null ? Optional.empty() : Optional.of(v);
    }

    /**
     * @param className binary name
     * @return digest of the jar version of a class that Lemon redefined at least once
     */
    public synchronized Optional<String> originalSha(String className) {
        return Optional.ofNullable(this.originalSha.get(className));
    }

    /** @return monotonic counter, incremented on every change */
    public synchronized long version() {
        return this.version;
    }

    /** @return every attempt in this JVM, oldest first */
    public synchronized List<PatchRecord> records() {
        return Collections.unmodifiableList(new ArrayList<>(this.records));
    }

    /**
     * @param id patch id
     * @return the latest record of that patch
     */
    public synchronized Optional<PatchRecord> latest(String id) {
        for (int i = this.records.size() - 1; i >= 0; i--) {
            if (this.records.get(i).id().equals(id)) {
                return Optional.of(this.records.get(i));
            }
        }
        return Optional.empty();
    }

    /** @return number of active patches */
    public synchronized long activeCount() {
        return this.records.stream().filter(r -> r.status() == Status.ACTIVE).count();
    }

    synchronized void applied(PatchRecord record) {
        for (ClassRecord c : record.classes()) {
            if (c.previous().installedBy() == null) {
                this.originalSha.putIfAbsent(c.name(), c.previous().sha256());
            }
            this.current.put(c.name(), c.installed());
        }
        this.records.add(record);
        this.version++;
    }

    synchronized void failed(PatchRecord record) {
        this.records.add(record);
        this.version++;
    }

    synchronized void rolledBack(String id, String detail) {
        for (int i = this.records.size() - 1; i >= 0; i--) {
            PatchRecord r = this.records.get(i);
            if (r.id().equals(id) && r.status() == Status.ACTIVE) {
                for (ClassRecord c : r.classes()) {
                    this.current.put(c.name(), c.previous());
                }
                this.records.set(i, r.with(Status.ROLLED_BACK, detail));
                this.version++;
                return;
            }
        }
        throw new IllegalStateException("patch " + id + " is not active");
    }

    /** Test hook: forget everything (the JVM keeps its classes). */
    synchronized void clearForTests() {
        this.current.clear();
        this.originalSha.clear();
        this.records.clear();
        this.version++;
    }
}
