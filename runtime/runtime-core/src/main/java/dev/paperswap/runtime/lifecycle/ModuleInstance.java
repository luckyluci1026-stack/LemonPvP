package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.ModuleState;
import dev.paperswap.runtime.api.RuntimeModule;
import dev.paperswap.runtime.config.ModuleConfigHolder;
import dev.paperswap.runtime.loader.ModuleArtifact;
import dev.paperswap.runtime.loader.ModuleClassLoader;
import dev.paperswap.runtime.platform.CallbackGuard;
import dev.paperswap.runtime.platform.PlatformBinding;
import dev.paperswap.runtime.resources.ResourceTracker;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * One loaded version of a module. A hot swap creates a new instance; the old one is
 * unloaded afterwards. State changes happen on the main thread only; reads may happen on
 * any thread.
 */
final class ModuleInstance {

    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final int HISTORY = 16;

    private final long sequence = SEQUENCE.incrementAndGet();
    private final ModuleArtifact artifact;
    private final String sha256;
    private final Path stagedJar;
    private final ModuleConfigHolder config;
    private final Logger logger;
    private final Deque<String> history = new ArrayDeque<>();

    private volatile ModuleState state = ModuleState.DISCOVERED;
    private ModuleClassLoader classLoader;
    private RuntimeModule module;
    private DefaultModuleContext context;
    private volatile ResourceTracker tracker;
    private volatile CallbackGuard callbacks;
    private volatile PlatformBinding binding;

    private volatile long loadMillis = -1;
    private volatile long enableMillis = -1;
    private volatile long disableMillis = -1;
    private volatile Instant enabledSince;
    private volatile String lastError;
    private volatile List<String> cleanupProblems = List.of();
    private volatile List<String> linkedDependencies = List.of();

    ModuleInstance(ModuleArtifact artifact, String sha256, Path stagedJar, ModuleConfigHolder config, Logger logger) {
        this.artifact = artifact;
        this.sha256 = sha256;
        this.stagedJar = stagedJar;
        this.config = config;
        this.logger = logger;
        record(ModuleState.DISCOVERED, null);
    }

    long sequence() {
        return this.sequence;
    }

    ModuleArtifact artifact() {
        return this.artifact;
    }

    ModuleDescriptor descriptor() {
        return this.artifact.descriptor();
    }

    String id() {
        return this.artifact.id();
    }

    String sha256() {
        return this.sha256;
    }

    Path stagedJar() {
        return this.stagedJar;
    }

    ModuleConfigHolder config() {
        return this.config;
    }

    Logger logger() {
        return this.logger;
    }

    ModuleState state() {
        return this.state;
    }

    /**
     * Performs a checked state transition.
     *
     * @param to   target state
     * @param note optional note for the history
     */
    synchronized void transition(ModuleState to, String note) {
        ModuleStateMachine.check(this.state, to);
        this.state = to;
        record(to, note);
    }

    private synchronized void record(ModuleState s, String note) {
        if (this.history.size() == HISTORY) {
            this.history.removeFirst();
        }
        this.history.addLast(Instant.now() + " " + s + (note == null ? "" : " (" + note + ")"));
    }

    synchronized List<String> history() {
        return new ArrayList<>(this.history);
    }

    ModuleClassLoader classLoader() {
        return this.classLoader;
    }

    void classLoader(ModuleClassLoader loader) {
        this.classLoader = loader;
    }

    RuntimeModule module() {
        return this.module;
    }

    void module(RuntimeModule module) {
        this.module = module;
    }

    DefaultModuleContext context() {
        return this.context;
    }

    void context(DefaultModuleContext context) {
        this.context = context;
    }

    ResourceTracker tracker() {
        return this.tracker;
    }

    void tracker(ResourceTracker tracker) {
        this.tracker = tracker;
    }

    CallbackGuard callbacks() {
        return this.callbacks;
    }

    void callbacks(CallbackGuard guard) {
        this.callbacks = guard;
    }

    PlatformBinding binding() {
        return this.binding;
    }

    void binding(PlatformBinding binding) {
        this.binding = binding;
    }

    long loadMillis() {
        return this.loadMillis;
    }

    void loadMillis(long v) {
        this.loadMillis = v;
    }

    long enableMillis() {
        return this.enableMillis;
    }

    void enableMillis(long v) {
        this.enableMillis = v;
    }

    long disableMillis() {
        return this.disableMillis;
    }

    void disableMillis(long v) {
        this.disableMillis = v;
    }

    Instant enabledSince() {
        return this.enabledSince;
    }

    void enabledSince(Instant v) {
        this.enabledSince = v;
    }

    String lastError() {
        return this.lastError;
    }

    void lastError(String v) {
        this.lastError = v;
    }

    List<String> cleanupProblems() {
        return this.cleanupProblems;
    }

    void cleanupProblems(List<String> v) {
        this.cleanupProblems = List.copyOf(v);
    }

    /** @return ids of modules whose class loaders this instance links to */
    List<String> linkedDependencies() {
        return this.linkedDependencies;
    }

    void linkedDependencies(List<String> ids) {
        this.linkedDependencies = List.copyOf(ids);
    }

    /** Drops strong references to module objects after the class loader was closed. */
    void releaseReferences() {
        this.module = null;
        this.context = null;
        this.binding = null;
        this.callbacks = null;
        this.tracker = null;
        this.classLoader = null;
    }
}
