package dev.lemonpvp.runtime.loader;

import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.api.RuntimeApi;
import dev.lemonpvp.runtime.descriptor.DescriptorException;
import dev.lemonpvp.runtime.descriptor.DescriptorParser;
import dev.lemonpvp.runtime.security.PathGuard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Discovers module jars. Reads only the descriptor of each jar - no class is loaded and
 * nothing is executed. Must not run on the server main thread.
 */
public final class ModuleScanner {

    /** Maximum size of a module jar (64 MiB). */
    public static final long MAX_JAR_SIZE = 64L * 1024 * 1024;
    /** Maximum size of the descriptor entry (64 KiB). */
    public static final long MAX_DESCRIPTOR_SIZE = 64L * 1024;
    /** Maximum number of jar files examined per scan. */
    public static final int MAX_FILES = 1024;

    private final Path moduleDirectory;

    /** @param moduleDirectory directory containing module jars */
    public ModuleScanner(Path moduleDirectory) {
        this.moduleDirectory = moduleDirectory.toAbsolutePath().normalize();
    }

    /** @return the scanned directory */
    public Path moduleDirectory() {
        return this.moduleDirectory;
    }

    /**
     * Scans the module directory (non-recursive).
     *
     * @return the scan report; duplicate module ids are reported and excluded entirely
     * @throws IOException if the directory cannot be listed
     */
    public ScanReport scan() throws IOException {
        List<ScanReport.Problem> problems = new ArrayList<>();
        Map<String, List<ModuleArtifact>> byId = new TreeMap<>();
        if (!Files.isDirectory(this.moduleDirectory)) {
            return new ScanReport(List.of(), List.of(), Instant.now());
        }
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.moduleDirectory, "*.jar")) {
            for (Path file : stream) {
                if (++count > MAX_FILES) {
                    problems.add(new ScanReport.Problem("*", null, "more than " + MAX_FILES + " jar files, remaining files ignored"));
                    break;
                }
                String name = file.getFileName().toString();
                try {
                    ModuleArtifact artifact = inspect(file);
                    byId.computeIfAbsent(artifact.id(), k -> new ArrayList<>()).add(artifact);
                } catch (DescriptorException e) {
                    problems.add(new ScanReport.Problem(PathGuard.sanitize(name), null, "invalid descriptor: " + e.getMessage()));
                } catch (SecurityException | IOException e) {
                    problems.add(new ScanReport.Problem(PathGuard.sanitize(name), null, e.getMessage()));
                }
            }
        }
        List<ModuleArtifact> artifacts = new ArrayList<>();
        for (Map.Entry<String, List<ModuleArtifact>> e : byId.entrySet()) {
            if (e.getValue().size() == 1) {
                artifacts.add(e.getValue().getFirst());
            } else {
                // Ambiguous: refusing all candidates is safer than guessing which one is meant.
                for (ModuleArtifact a : e.getValue()) {
                    problems.add(new ScanReport.Problem(PathGuard.sanitize(a.file().getFileName().toString()), e.getKey(),
                            "duplicate module id '" + e.getKey() + "' in " + e.getValue().size()
                                    + " files; remove all but one"));
                }
            }
        }
        return new ScanReport(artifacts, problems, Instant.now());
    }

    /**
     * Inspects a single jar.
     *
     * @param file jar file inside the module directory
     * @return the artifact
     * @throws IOException         if the jar cannot be read
     * @throws DescriptorException if the descriptor is missing or invalid
     * @throws SecurityException   if the file name or location is unsafe
     */
    public ModuleArtifact inspect(Path file) throws IOException, DescriptorException {
        String name = file.getFileName().toString();
        if (!PathGuard.isSafeFileName(name)) {
            throw new SecurityException("unsafe file name");
        }
        PathGuard.requireInside(this.moduleDirectory, file);
        PathGuard.requireRegularFile(file);
        long size = Files.size(file);
        if (size > MAX_JAR_SIZE) {
            throw new IOException("jar is larger than " + MAX_JAR_SIZE + " bytes");
        }
        ModuleDescriptor descriptor;
        try (JarFile jar = new JarFile(file.toFile(), false)) {
            ZipEntry full = jar.getEntry(RuntimeApi.DESCRIPTOR_FILE);
            ZipEntry shortName = jar.getEntry(RuntimeApi.DESCRIPTOR_FILE_SHORT);
            if (full != null && shortName != null) {
                throw new DescriptorException("contains both " + RuntimeApi.DESCRIPTOR_FILE + " and " + RuntimeApi.DESCRIPTOR_FILE_SHORT
                        + "; keep only one");
            }
            ZipEntry entry = full != null ? full : shortName;
            if (entry == null || entry.isDirectory()) {
                throw new DescriptorException("missing " + RuntimeApi.DESCRIPTOR_FILE_SHORT + " (or " + RuntimeApi.DESCRIPTOR_FILE
                        + "); is this a module jar? Bukkit plugins belong in plugins/");
            }
            if (entry.getSize() > MAX_DESCRIPTOR_SIZE) {
                throw new DescriptorException(entry.getName() + " is larger than " + MAX_DESCRIPTOR_SIZE + " bytes");
            }
            try (InputStream in = new LimitedInputStream(jar.getInputStream(entry), MAX_DESCRIPTOR_SIZE)) {
                descriptor = DescriptorParser.parse(in, entry.getName());
            }
        } catch (ZipException e) {
            throw new IOException("not a valid jar: " + e.getMessage(), e);
        }
        return new ModuleArtifact(file.toAbsolutePath().normalize(), size, descriptor);
    }
}
