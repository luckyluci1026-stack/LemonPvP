package dev.lemonpvp.runtime.livepatch.tool;

import dev.lemonpvp.runtime.livepatch.BuildIdentity;
import dev.lemonpvp.runtime.livepatch.ChangeKind;
import dev.lemonpvp.runtime.livepatch.ClassChange;
import dev.lemonpvp.runtime.livepatch.ClassDiff;
import dev.lemonpvp.runtime.livepatch.PatchArchive;
import dev.lemonpvp.runtime.livepatch.PatchIds;
import dev.lemonpvp.runtime.livepatch.PatchManifest;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFormatException;
import dev.lemonpvp.runtime.livepatch.classfile.ClassInfo;
import dev.lemonpvp.runtime.security.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Builds a core live patch on the developer machine.
 *
 * <pre>
 * java -cp &lt;server classes&gt; dev.lemonpvp.runtime.livepatch.tool.LivePatchTool create \
 *      --base &lt;deployed server jar&gt; --classes &lt;compiled classes dir or jar&gt; \
 *      [--reference &lt;jar built from the unchanged sources&gt;] \
 *      [--id fix-tps] [--description "..."] [--out build/livepatch] \
 *      [--target-build &lt;build id&gt;] [--minecraft &lt;version&gt;]
 * </pre>
 *
 * <p>Every class whose bytes differ from the reference jar becomes part of the patch
 * (classes that do not exist there are added as new classes). The reference defaults to
 * the base jar. The base jar must be the exact jar that runs on the server: its class
 * digests go into the patch, and the server rejects a class whose running bytecode differs
 * from them. Classes that exist in the base jar but not in {@code --classes} are ignored (a
 * patch cannot remove classes).</p>
 *
 * <p>{@code --reference} is for builds whose server jar is rewritten after compilation:
 * the 1.21.11 paperclip produces a server jar whose class files have a different constant
 * pool layout than the Gradle jar (same classes, same code). Changes are then detected
 * against the Gradle jar ({@code --reference}), digests are taken from the server jar
 * ({@code --base}), and every patched class must be semantically identical in both
 * (otherwise the reference is not the build that runs).</p>
 *
 * <p>Uses only the JDK, so it runs from the server's own compiled classes.</p>
 */
public final class LivePatchTool {

    /** Tool version written into patches. */
    public static final String VERSION = "lemon-livepatch-tool 1";
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private LivePatchTool() {
    }

    /**
     * @param args command line
     */
    public static void main(String[] args) {
        int code;
        try {
            code = run(args, System.out);
        } catch (ToolException e) {
            System.err.println("error: " + e.getMessage());
            code = 1;
        } catch (IOException e) {
            System.err.println("error: " + e);
            code = 1;
        }
        System.exit(code);
    }

    /** A usage or input problem. */
    public static final class ToolException extends Exception {
        /** @param message problem */
        public ToolException(String message) {
            super(message);
        }
    }

    /**
     * Runs the tool.
     *
     * @param args arguments
     * @param out  output
     * @return exit code: 0 patch written, 2 nothing changed
     * @throws ToolException on bad input
     * @throws IOException   on I/O errors
     */
    public static int run(String[] args, PrintStream out) throws ToolException, IOException {
        if (args.length == 0 || !args[0].equals("create")) {
            throw new ToolException("usage: create --base <server jar> --classes <dir|jar> [--reference <jar>] [--id <id>]"
                    + " [--description <text>] [--out <dir>] [--target-build <id>] [--minecraft <version>]");
        }
        Map<String, String> opts = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--") || i + 1 >= args.length) {
                throw new ToolException("expected --option value, got " + args[i]);
            }
            opts.put(args[i].substring(2), args[++i]);
        }
        Path base = Path.of(required(opts, "base"));
        Path classes = Path.of(required(opts, "classes"));
        Path reference = opts.containsKey("reference") ? Path.of(opts.get("reference")) : base;
        String id = opts.getOrDefault("id", "p-" + ID_TIME.format(Instant.now()));
        if (PatchIds.problem(id) != null) {
            throw new ToolException("--id: " + PatchIds.problem(id));
        }
        Path outDir = Path.of(opts.getOrDefault("out", "."));
        String buildId = opts.getOrDefault("target-build", BuildIdentity.of(base));
        String minecraft = opts.getOrDefault("minecraft", minecraftOf(base));
        if (BuildIdentity.UNKNOWN.equals(buildId)) {
            out.println("warning: the base jar has no build manifest; pass --target-build (see /fork live patch status)");
        }

        Map<String, String> baseSha = classDigests(base);
        if (baseSha.isEmpty()) {
            throw new ToolException("the base jar contains no classes: " + base);
        }
        Map<String, String> referenceSha = reference.equals(base) ? baseSha : classDigests(reference);
        if (!reference.equals(base)) {
            String refBuild = BuildIdentity.of(reference);
            if (!refBuild.equals(buildId) && !opts.containsKey("target-build")) {
                throw new ToolException("--reference is build " + refBuild + " but --base is build " + buildId
                        + "; both must come from the same build");
            }
        }

        Map<String, byte[]> changed = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : compiledClasses(classes).entrySet()) {
            String sha = Hashing.sha256(e.getValue());
            if (!sha.equals(referenceSha.get(e.getKey()))) {
                changed.put(e.getKey(), e.getValue());
            }
        }
        if (changed.isEmpty()) {
            out.println("no class differs from the base jar - nothing to patch");
            return 2;
        }
        if (changed.size() > PatchManifest.MAX_CLASSES) {
            throw new ToolException(changed.size() + " classes differ from the " + (reference.equals(base) ? "base" : "reference")
                    + " jar (limit " + PatchManifest.MAX_CLASSES + "); was it built from these sources? If the server jar is"
                    + " rewritten by the build (1.21.11 paperclip), pass the Gradle jar as --reference and the server jar as --base");
        }

        List<PatchManifest.Entry> entries = new ArrayList<>();
        boolean structural = false;
        out.println("patch " + id + " for " + buildId + " (Minecraft " + minecraft + ")");
        if (!reference.equals(base)) {
            out.println("  changes detected against " + reference.getFileName() + ", digests taken from " + base.getFileName());
        }
        for (Map.Entry<String, byte[]> e : changed.entrySet()) {
            String name = e.getKey();
            String problem = PatchIds.classProblem(name);
            if (problem != null) {
                throw new ToolException(name + ": " + problem);
            }
            ClassInfo next;
            try {
                next = ClassFileReader.read(e.getValue());
            } catch (ClassFormatException ex) {
                throw new ToolException(name + ": " + ex.getMessage());
            }
            if (!next.javaName().equals(name)) {
                throw new ToolException("file for " + name + " declares " + next.javaName());
            }
            boolean isNew = !baseSha.containsKey(name);
            if (isNew && referenceSha.containsKey(name)) {
                throw new ToolException(name + " is in --reference but not in --base; they are not the same build");
            }
            entries.add(new PatchManifest.Entry(name, Hashing.sha256(e.getValue()), isNew ? null : baseSha.get(name), isNew));
            if (isNew) {
                out.println("  new      " + name);
                continue;
            }
            List<ClassChange> changes;
            try {
                ClassInfo running = ClassFileReader.read(readEntry(base, name));
                if (!reference.equals(base) && referenceSha.containsKey(name)
                        && !baseSha.get(name).equals(referenceSha.get(name))) {
                    List<ClassChange> drift = ClassDiff.compare(running, ClassFileReader.read(readEntry(reference, name)));
                    if (!drift.isEmpty()) {
                        throw new ToolException(name + " differs between --base and --reference beyond the class file layout ("
                                + drift.get(0) + "); --reference is not the build that runs on the server");
                    }
                }
                changes = ClassDiff.compare(running, next);
            } catch (ClassFormatException ex) {
                throw new ToolException("base class " + name + ": " + ex.getMessage());
            }
            out.println("  changed  " + name);
            for (ClassChange c : changes) {
                boolean body = c.kind().bodyOnly();
                structural |= !body;
                out.println("           " + (body ? "" : "[structural] ") + c);
            }
            if (changes.isEmpty()) {
                out.println("           " + ChangeKind.ATTRIBUTES_ONLY.description());
            }
        }
        PatchManifest manifest = new PatchManifest(id, opts.getOrDefault("description", ""), Instant.now().toString(), VERSION,
                buildId, minecraft, entries);
        byte[] archive = PatchArchive.write(manifest, changed);
        Files.createDirectories(outDir);
        Path file = outDir.resolve(id + PatchIds.SUFFIX);
        Path tmp = outDir.resolve(id + PatchIds.SUFFIX + ".tmp");
        Files.write(tmp, archive);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(outDir.resolve(file.getFileName() + ".sha256"), Hashing.sha256(archive) + "  " + file.getFileName() + "\n");
        out.println("wrote " + file + " (" + archive.length + " bytes, sha256 " + Hashing.sha256(archive) + ")");
        out.println("      " + file + ".sha256");
        if (structural) {
            out.println("note: [structural] changes need a JVM with enhanced class redefinition; a standard JVM rejects"
                    + " this patch (the server tells you before changing anything)");
        }
        out.println("next: upload both files to the server's patch directory, then /fork live check " + id);
        return 0;
    }

    private static Map<String, String> classDigests(Path jarFile) throws IOException {
        Map<String, String> out = new HashMap<>();
        try (JarFile jar = new JarFile(jarFile.toFile(), false)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = className(e.getName());
                if (name == null) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(e)) {
                    out.put(name, Hashing.sha256(in));
                }
            }
        }
        return out;
    }

    private static byte[] readEntry(Path jarFile, String className) throws IOException {
        try (JarFile jar = new JarFile(jarFile.toFile(), false)) {
            JarEntry e = jar.getJarEntry(className.replace('.', '/') + ".class");
            try (InputStream in = jar.getInputStream(e)) {
                return in.readAllBytes();
            }
        }
    }

    private static String required(Map<String, String> opts, String key) throws ToolException {
        String v = opts.get(key);
        if (v == null || v.isBlank()) {
            throw new ToolException("--" + key + " is required");
        }
        return v;
    }

    /** {@code 26.2-DEV-4eff375} -> {@code 26.2}. */
    private static String minecraftOf(Path base) throws IOException {
        try (JarFile jar = new JarFile(base.toFile(), false)) {
            String v = jar.getManifest() == null ? null : jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            if (v == null || v.isBlank()) {
                return "unknown";
            }
            int dash = v.indexOf('-');
            return dash > 0 ? v.substring(0, dash) : v;
        }
    }

    private static String className(String entry) {
        if (!entry.endsWith(".class") || entry.startsWith("META-INF/") || entry.endsWith("module-info.class")
                || entry.endsWith("package-info.class")) {
            return null;
        }
        return entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
    }

    private static Map<String, byte[]> compiledClasses(Path classes) throws IOException, ToolException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        if (Files.isDirectory(classes)) {
            List<Path> files;
            try (Stream<Path> s = Files.walk(classes)) {
                files = s.filter(p -> p.toString().endsWith(".class")).sorted().toList();
            }
            for (Path p : files) {
                String name = className(classes.relativize(p).toString().replace('\\', '/'));
                if (name != null) {
                    out.put(name, Files.readAllBytes(p));
                }
            }
        } else if (Files.isRegularFile(classes)) {
            try (JarFile jar = new JarFile(classes.toFile(), false)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    String name = className(e.getName());
                    if (name != null) {
                        try (InputStream in = jar.getInputStream(e)) {
                            out.put(name, in.readAllBytes());
                        }
                    }
                }
            }
        } else {
            throw new ToolException("--classes " + classes + " does not exist");
        }
        return out;
    }
}
