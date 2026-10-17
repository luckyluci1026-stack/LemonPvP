package dev.paperswap.runtime.lifecycle;

import dev.paperswap.runtime.api.ConfigSnapshot;
import dev.paperswap.runtime.api.ModuleDescriptor;
import dev.paperswap.runtime.api.RuntimeApi;
import dev.paperswap.runtime.config.ModuleConfigHolder;
import dev.paperswap.runtime.config.RuntimeConfig;
import dev.paperswap.runtime.config.YamlSupport;
import dev.paperswap.runtime.descriptor.MinecraftVersionMatcher;
import dev.paperswap.runtime.loader.ArtifactStager;
import dev.paperswap.runtime.loader.ClassExposurePolicy;
import dev.paperswap.runtime.loader.ClassPreloader;
import dev.paperswap.runtime.loader.LimitedInputStream;
import dev.paperswap.runtime.loader.ModuleArtifact;
import dev.paperswap.runtime.loader.ModuleClassLoader;
import dev.paperswap.runtime.loader.ModuleScanner;
import dev.paperswap.runtime.loader.ScanReport;
import dev.paperswap.runtime.security.IntegrityVerifier;
import dev.paperswap.runtime.security.PathGuard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Worker-thread part of loading a module: discovery, compatibility and integrity checks,
 * staging, default resources, configuration parsing, class loader creation and class
 * preloading. Nothing here executes module code.
 */
final class ModulePreparer {

    /** Upper bound for one default resource copied from a module jar. */
    private static final long MAX_RESOURCE_SIZE = 1024 * 1024;

    private final RuntimeEnvironment env;

    ModulePreparer(RuntimeEnvironment env) {
        this.env = env;
    }

    /**
     * A module that passed every check and is ready to be instantiated.
     *
     * @param artifact        discovered artifact
     * @param sha256          verified digest
     * @param stagedJar       private copy the class loader reads
     * @param dataDirectory   module data directory
     * @param configCandidate parsed configuration
     * @param details         check results and warnings
     */
    record Prepared(ModuleArtifact artifact, String sha256, Path stagedJar, Path dataDirectory,
                    ConfigSnapshot configCandidate, List<String> details) {
    }

    /** @return a fresh scan of the module directory */
    ScanReport scan() throws IOException {
        RuntimeConfig config = this.env.config().get();
        return new ModuleScanner(this.env.paths().moduleDirectory(config)).scan();
    }

    /**
     * Finds and checks a module without changing anything on disk except the staging copy.
     *
     * @param id             module id
     * @param expectedSha256 digest the jar must have (persisted activations) or {@code null}
     * @param stage          whether to stage the jar and prepare the data directory
     * @return the prepared module
     * @throws LifecycleException if any check fails
     */
    Prepared prepare(String id, String expectedSha256, boolean stage) throws LifecycleException {
        RuntimeConfig config = this.env.config().get();
        List<String> details = new ArrayList<>();
        ScanReport report;
        try {
            report = scan();
        } catch (IOException | SecurityException e) {
            throw new LifecycleException("cannot scan module directory: " + e.getMessage());
        }
        ModuleArtifact artifact = report.find(id).orElse(null);
        if (artifact == null) {
            List<String> problems = report.problemsFor(id).stream().map(p -> p.file() + ": " + p.message()).toList();
            if (problems.isEmpty()) {
                problems = report.problems().stream().filter(p -> p.moduleId() == null)
                        .map(p -> p.file() + ": " + p.message()).toList();
            }
            throw new LifecycleException("no valid module with id '" + id + "' in " + config.moduleDirectory(), problems);
        }
        ModuleDescriptor d = artifact.descriptor();
        List<String> compat = compatibilityProblems(d, config);
        if (!compat.isEmpty()) {
            throw new LifecycleException(d.coordinates() + " is not compatible with this server", compat);
        }
        details.add("descriptor ok: " + d.coordinates() + " (api " + d.apiVersion() + ", minecraft " + d.minecraftVersions() + ")");

        IntegrityVerifier verifier = new IntegrityVerifier(config.verifyIntegrity(), this.env.paths().trustedHashes(config));
        IntegrityVerifier.Verified verified;
        try {
            verified = verifier.verify(artifact.file());
        } catch (IntegrityVerifier.IntegrityException | SecurityException e) {
            throw new LifecycleException("integrity check failed for " + d.coordinates() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new LifecycleException("cannot read " + artifact.file().getFileName() + ": " + e.getMessage());
        }
        details.addAll(verified.checks());
        if (expectedSha256 != null && !expectedSha256.equals(verified.sha256())) {
            throw new LifecycleException("the jar of '" + id + "' changed since it was activated or checked (expected "
                    + expectedSha256 + ", found " + verified.sha256() + "); check it again or activate it explicitly with"
                    + " /fork module enable " + id);
        }
        if (!stage) {
            return new Prepared(artifact, verified.sha256(), null, null, ConfigSnapshot.empty(), details);
        }

        Path staged;
        try {
            staged = new ArtifactStager(this.env.paths().staging()).stage(artifact, verified.sha256());
        } catch (IntegrityVerifier.IntegrityException | IOException | SecurityException e) {
            throw new LifecycleException("cannot stage " + d.coordinates() + ": " + e.getMessage());
        }
        Path dataDir;
        ConfigSnapshot candidate;
        try {
            dataDir = this.env.paths().moduleData(id);
            Files.createDirectories(dataDir);
            details.addAll(copyDefaultResources(staged, d, dataDir));
            candidate = new ModuleConfigHolder(dataDir.resolve(ModuleConfigHolder.FILE_NAME)).readCandidate();
        } catch (IOException | SecurityException | YamlSupport.YamlFormatException e) {
            throw new LifecycleException("cannot prepare data of " + d.coordinates() + ": " + e.getMessage());
        }
        return new Prepared(artifact, verified.sha256(), staged, dataDir, candidate, details);
    }

    /**
     * @param d      descriptor
     * @param config runtime configuration
     * @return compatibility problems
     */
    List<String> compatibilityProblems(ModuleDescriptor d, RuntimeConfig config) {
        List<String> problems = new ArrayList<>();
        if (d.apiVersion() > RuntimeApi.API_VERSION) {
            problems.add("requires module API " + d.apiVersion() + " but this runtime provides " + RuntimeApi.API_VERSION);
        } else if (!config.allowedApiVersions().contains(d.apiVersion())) {
            problems.add("module API " + d.apiVersion() + " is not in allowed-api-versions " + config.allowedApiVersions());
        }
        String mc = this.env.platform().minecraftVersion();
        if (!MinecraftVersionMatcher.matchesAny(d.minecraftVersions(), mc)) {
            problems.add("supports Minecraft " + d.minecraftVersions() + " but the server runs " + mc);
        }
        return problems;
    }

    private static List<String> copyDefaultResources(Path stagedJar, ModuleDescriptor d, Path dataDir) throws IOException {
        List<String> out = new ArrayList<>();
        if (d.resources().isEmpty()) {
            return out;
        }
        try (JarFile jar = new JarFile(stagedJar.toFile(), false)) {
            for (String name : d.resources()) {
                Path target = PathGuard.resolveInside(dataDir, name);
                if (Files.exists(target)) {
                    continue; // never overwrite administrator changes
                }
                ZipEntry entry = jar.getEntry(name);
                if (entry == null || entry.isDirectory()) {
                    out.add("declared resource '" + name + "' not found in jar");
                    continue;
                }
                if (entry.getSize() > MAX_RESOURCE_SIZE) {
                    throw new IOException("resource '" + name + "' is larger than " + MAX_RESOURCE_SIZE + " bytes");
                }
                Files.createDirectories(target.getParent());
                Path tmp = Files.createTempFile(target.getParent(), ".resource-", ".tmp");
                try (InputStream in = new LimitedInputStream(jar.getInputStream(entry), MAX_RESOURCE_SIZE)) {
                    long copied = Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    if (copied > MAX_RESOURCE_SIZE) {
                        throw new IOException("resource '" + name + "' is larger than " + MAX_RESOURCE_SIZE + " bytes");
                    }
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                    out.add("created default resource " + name);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
        }
        return out;
    }

    /**
     * Creates the class loader and defines all classes (worker thread).
     *
     * @param prepared      prepared module
     * @param dependencies  class loaders of linked module dependencies
     * @param pluginLoaders class loaders of plugin dependencies
     * @return the loader and preload warnings
     * @throws LifecycleException if the jar cannot be read
     */
    LoaderResult createLoader(Prepared prepared, List<ModuleClassLoader> dependencies, List<ClassLoader> pluginLoaders)
            throws LifecycleException {
        RuntimeConfig config = this.env.config().get();
        ModuleClassLoader loader = null;
        try {
            loader = new ModuleClassLoader(prepared.artifact().id(), prepared.stagedJar(),
                    this.env.platform().serverClassLoader(), dependencies, pluginLoaders,
                    new ClassExposurePolicy(config.allowServerInternals()));
            ClassPreloader.Result r = ClassPreloader.preload(loader, prepared.stagedJar());
            List<String> warnings = new ArrayList<>();
            warnings.add("defined " + r.defined() + " classes off the main thread");
            for (String w : r.warnings()) {
                warnings.add("class not loadable: " + w);
            }
            return new LoaderResult(loader, warnings);
        } catch (IOException e) {
            if (loader != null) {
                try {
                    loader.close();
                } catch (IOException ignored) {
                    // best effort
                }
            }
            throw new LifecycleException("cannot open " + prepared.stagedJar().getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * @param loader   created class loader
     * @param warnings preload warnings
     */
    record LoaderResult(ModuleClassLoader loader, List<String> warnings) {
    }
}
