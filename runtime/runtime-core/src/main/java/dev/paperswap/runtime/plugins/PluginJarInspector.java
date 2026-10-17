package dev.paperswap.runtime.plugins;

import dev.paperswap.runtime.config.YamlSupport;
import dev.paperswap.runtime.livepatch.classfile.ClassFileReader;
import dev.paperswap.runtime.livepatch.classfile.ClassFormatException;
import dev.paperswap.runtime.security.Hashing;
import dev.paperswap.runtime.security.IntegrityVerifier;
import dev.paperswap.runtime.security.PathGuard;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Reads a plugin jar the way Paper would decide what it is, without loading any class:
 * descriptor ({@code paper-plugin.yml} wins over {@code plugin.yml}, as in Paper), main
 * class presence, class file versions and the APIs the classes refer to.
 *
 * <p>File I/O and hashing: call on the worker thread.</p>
 */
public final class PluginJarInspector {

    /** Largest plugin jar that is inspected. */
    public static final long MAX_JAR_BYTES = 256L * 1024 * 1024;
    private static final int MAX_CLASSES = 50_000;
    private static final int MAX_CLASS_BYTES = 8 * 1024 * 1024;
    private static final int MAX_REPORTED = 5;

    private PluginJarInspector() {
    }

    /**
     * Reads only the descriptor (fast path for matching jars to plugin names).
     *
     * @param jar plugin jar
     * @return the descriptor, or empty if the jar has none or it is unreadable
     */
    public static Optional<PluginInfo> readInfo(Path jar) {
        try (JarFile file = new JarFile(jar.toFile(), false)) {
            ZipEntry paper = file.getEntry("paper-plugin.yml");
            ZipEntry entry = paper != null ? paper : file.getEntry("plugin.yml");
            if (entry == null) {
                return Optional.empty();
            }
            byte[] data;
            try (InputStream in = file.getInputStream(entry)) {
                data = in.readNBytes(YamlSupport.MAX_DOCUMENT_BYTES + 1);
            }
            if (data.length > YamlSupport.MAX_DOCUMENT_BYTES) {
                return Optional.empty();
            }
            return Optional.of(PluginInfo.parse(YamlSupport.load(new ByteArrayInputStream(data), entry.getName()),
                    paper != null ? PluginInfo.Kind.PAPER : PluginInfo.Kind.BUKKIT));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * @param jar             plugin jar
     * @param javaFeature     feature version of the running JVM (e.g. 21)
     * @param trustedHashes   optional allow-list file
     * @return the inspection result (problems are reported, not thrown)
     * @throws IOException if the file cannot be read at all
     */
    public static PluginJar inspect(Path jar, int javaFeature, Optional<Path> trustedHashes) throws IOException {
        PathGuard.requireRegularFile(jar);
        long size = Files.size(jar);
        if (size > MAX_JAR_BYTES) {
            return new PluginJar(jar, "", size, null, List.of("jar is larger than " + MAX_JAR_BYTES + " bytes"),
                    List.of(), List.of(), 0, 0);
        }
        String sha = Hashing.sha256(jar);
        List<String> problems = new ArrayList<>();
        Path sidecar = jar.resolveSibling(jar.getFileName() + ".sha256");
        if (Files.exists(sidecar) || trustedHashes.isPresent()) {
            try {
                new IntegrityVerifier(Files.exists(sidecar), trustedHashes).verifyDigest(jar, sha);
            } catch (IntegrityVerifier.IntegrityException e) {
                problems.add("integrity: " + e.getMessage());
            }
        }
        PluginInfo info = null;
        List<String> lifecycle = new ArrayList<>();
        List<String> internals = new ArrayList<>();
        int maxVersion = 0;
        int classes = 0;
        try (JarFile file = new JarFile(jar.toFile(), false)) {
            ZipEntry paper = file.getEntry("paper-plugin.yml");
            ZipEntry bukkit = file.getEntry("plugin.yml");
            ZipEntry descriptor = paper != null ? paper : bukkit;
            if (descriptor == null) {
                problems.add("no plugin.yml or paper-plugin.yml - not a plugin jar"
                        + (file.getEntry("paperswap-module.yml") != null || file.getEntry("module.yml") != null
                        || file.getEntry("lemon-module.yml") != null
                        ? " (this is a PaperSwap module; put it into the module directory)" : ""));
            } else {
                byte[] data;
                try (InputStream in = file.getInputStream(descriptor)) {
                    data = in.readNBytes(YamlSupport.MAX_DOCUMENT_BYTES + 1);
                }
                if (data.length > YamlSupport.MAX_DOCUMENT_BYTES) {
                    problems.add(descriptor.getName() + " is larger than " + YamlSupport.MAX_DOCUMENT_BYTES + " bytes");
                } else {
                    try {
                        info = PluginInfo.parse(YamlSupport.load(new ByteArrayInputStream(data), descriptor.getName()),
                                paper != null ? PluginInfo.Kind.PAPER : PluginInfo.Kind.BUKKIT);
                    } catch (YamlSupport.YamlFormatException | IllegalArgumentException e) {
                        problems.add(e.getMessage());
                    }
                }
            }
            if (info != null && file.getEntry(info.main().replace('.', '/') + ".class") == null) {
                problems.add("main class " + info.main() + " is not in the jar");
            }
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/")) {
                    continue;
                }
                if (++classes > MAX_CLASSES) {
                    problems.add("more than " + MAX_CLASSES + " classes; not inspected completely");
                    break;
                }
                byte[] bytes;
                try (InputStream in = file.getInputStream(e)) {
                    bytes = in.readNBytes(MAX_CLASS_BYTES + 1);
                }
                if (bytes.length > MAX_CLASS_BYTES) {
                    continue;
                }
                ClassFileReader.Constants c;
                try {
                    c = ClassFileReader.constants(bytes);
                } catch (ClassFormatException ex) {
                    continue; // obfuscated or non-standard class files are not our concern here
                }
                maxVersion = Math.max(maxVersion, c.majorVersion());
                String cls = name.substring(0, name.length() - 6).replace('/', '.');
                boolean usesLifecycle = false;
                boolean usesInternals = false;
                for (String s : c.utf8()) {
                    usesLifecycle |= s.contains("io/papermc/paper/plugin/lifecycle/event/");
                    usesInternals |= s.contains("net/minecraft/") || s.contains("org/bukkit/craftbukkit/");
                }
                if (usesLifecycle && lifecycle.size() < MAX_REPORTED) {
                    lifecycle.add(cls);
                }
                if (usesInternals && internals.size() < MAX_REPORTED) {
                    internals.add(cls);
                }
            }
        } catch (ZipException e) {
            problems.add("not a valid jar: " + e.getMessage());
        }
        int supported = javaFeature + 44;
        if (maxVersion > supported) {
            problems.add("contains classes for Java " + (maxVersion - 44) + " but the server runs Java " + javaFeature);
        }
        return new PluginJar(jar, sha, size, info, problems, lifecycle, internals, maxVersion, classes);
    }
}
