package dev.paperswap.runtime.livepatch.tool;

import dev.paperswap.runtime.livepatch.BuildIdentity;
import dev.paperswap.runtime.livepatch.ChangeKind;
import dev.paperswap.runtime.livepatch.ClassChange;
import dev.paperswap.runtime.livepatch.ClassDiff;
import dev.paperswap.runtime.livepatch.PatchArchive;
import dev.paperswap.runtime.livepatch.PatchIds;
import dev.paperswap.runtime.livepatch.PatchManifest;
import dev.paperswap.runtime.livepatch.classfile.ClassFileReader;
import dev.paperswap.runtime.livepatch.classfile.ClassFormatException;
import dev.paperswap.runtime.livepatch.classfile.ClassInfo;
import dev.paperswap.runtime.security.Hashing;
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
 * java -cp &lt;server classes&gt; dev.paperswap.runtime.livepatch.tool.LivePatchTool create \
 *      --base &lt;deployed server jar&gt; --classes &lt;compiled classes dir or jar&gt; \
 *      [--reference &lt;jar built from the unchanged sources&gt;] \
 *      [--id fix-tps] [--description "..."] [--out build/livepatch] \
 *      [--target-build &lt;build id&gt;] [--minecraft &lt;version&gt;]
 *
 * java -cp ... LivePatchTool check --patch &lt;id&gt;.swappatch --server-jar &lt;server jar&gt;
 * </pre>
 *
 * <p>{@code check} tests a patch file against a server jar before it is uploaded: checksum
 * file, archive structure, build id, Minecraft version, the digest of every replaced class,
 * and the kind of every change (standard JVM, enhanced redefinition, or never live). Exit
 * code 0: applicable on a standard JVM; 3: only with enhanced class redefinition; 1: not
 * applicable. The server checks everything again (including its JVM) before it applies a
 * patch.</p>
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
    public static final String VERSION = "paperswap-livepatch-tool 1";
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
     * @return exit code: create - 0 patch written, 2 nothing changed; check - 0 applicable on a standard JVM,
     * 3 only with enhanced class redefinition, 1 not applicable
     * @throws ToolException on bad input
     * @throws IOException   on I/O errors
     */
    public static int run(String[] args, PrintStream out) throws ToolException, IOException {
        if (args.length == 0 || !(args[0].equals("create") || args[0].equals("check"))) {
            throw new ToolException("usage: create --base <server jar> --classes <dir|jar> [--reference <jar>] [--id <id>]"
                    + " [--description <text>] [--out <dir>] [--target-build <id>] [--minecraft <version>]\n"
                    + "       check --patch <file.swappatch> --server-jar <server jar>");
        }
        Map<String, String> opts = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--") || i + 1 >= args.length) {
                throw new ToolException("expected --option value, got " + args[i]);
            }
            opts.put(args[i].substring(2), args[++i]);
        }
        if (args[0].equals("check")) {
            return check(Path.of(required(opts, "patch")), Path.of(required(opts, "server-jar")), out);
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
        Hierarchy hierarchy = new Hierarchy(base);
        Map<String, byte[]> included = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
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
            if (isNew) {
                entries.add(new PatchManifest.Entry(name, Hashing.sha256(e.getValue()), null, true));
                included.put(name, e.getValue());
                out.println("  new      " + name);
                continue;
            }
            List<ClassChange> changes;
            try {
                ClassInfo running = ClassFileReader.read(readEntry(base, name));
                boolean useReference = !reference.equals(base) && referenceSha.containsKey(name);
                // Show what the sources changed (same compiler output on both sides when a reference is given).
                changes = ClassDiff.compare(useReference ? ClassFileReader.read(readEntry(reference, name)) : running, next);
                if (changes.isEmpty()) {
                    // Same bytecode, only debug information, annotations or the class file layout differ
                    // (e.g. line numbers of an inner class whose outer source file changed): nothing to redefine.
                    skipped.add(name);
                    continue;
                }
                if (useReference && !baseSha.get(name).equals(referenceSha.get(name))) {
                    byte[] referenceBytes = readEntry(reference, name);
                    byte[] baseBytes = readEntry(base, name);
                    String drift = drift(baseBytes, referenceBytes, hierarchy);
                    if (drift != null) {
                        printDrift(out, name, baseBytes, referenceBytes);
                        throw new ToolException(name + " differs between --base and --reference beyond the class file layout ("
                                + drift + "); --reference is not the build that runs on the server");
                    }
                }
            } catch (ClassFormatException ex) {
                throw new ToolException("base class " + name + ": " + ex.getMessage());
            }
            entries.add(new PatchManifest.Entry(name, Hashing.sha256(e.getValue()), baseSha.get(name), false));
            included.put(name, e.getValue());
            out.println("  changed  " + name);
            for (ClassChange c : changes) {
                boolean body = c.kind().bodyOnly();
                structural |= !body;
                out.println("           " + (body ? "" : "[structural] ") + c);
            }
        }
        if (!skipped.isEmpty()) {
            out.println("  skipped " + skipped.size() + " class(es) without a bytecode change (debug information, annotations or"
                    + " class file layout only): " + (skipped.size() <= 10 ? skipped : skipped.subList(0, 10) + " ..."));
        }
        if (included.isEmpty()) {
            out.println("no class has a bytecode change - nothing to patch");
            return 2;
        }
        PatchManifest manifest = new PatchManifest(id, opts.getOrDefault("description", ""), Instant.now().toString(), VERSION,
                buildId, minecraft, entries);
        byte[] archive = PatchArchive.write(manifest, included);
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

    /**
     * Checks a patch file against a server jar without a server.
     *
     * @return 0 applicable on a standard JVM, 3 only with enhanced redefinition, 1 not applicable
     */
    static int check(Path patch, Path serverJar, PrintStream out) throws ToolException, IOException {
        if (!Files.isRegularFile(patch)) {
            throw new ToolException("no patch file " + patch);
        }
        if (!Files.isRegularFile(serverJar)) {
            throw new ToolException("no server jar " + serverJar);
        }
        if (Files.size(patch) > PatchArchive.MAX_ARCHIVE_BYTES) {
            throw new ToolException(patch + " is larger than " + PatchArchive.MAX_ARCHIVE_BYTES + " bytes");
        }
        byte[] bytes = Files.readAllBytes(patch);
        String sha = Hashing.sha256(bytes);
        List<String> problems = new ArrayList<>();
        out.println("check " + patch.getFileName() + " (sha256 " + sha + ") against " + serverJar.getFileName());
        Path sidecar = patch.resolveSibling(patch.getFileName() + ".sha256");
        if (Files.isRegularFile(sidecar)) {
            String expected = Files.readString(sidecar).trim().split("\\s+")[0];
            if (expected.equalsIgnoreCase(sha)) {
                out.println("  checksum file matches");
            } else {
                problems.add("checksum file " + sidecar.getFileName() + " says " + expected + ", the file has " + sha);
            }
        } else {
            out.println("  warning: no " + sidecar.getFileName() + " next to the patch (the server needs it when integrity checks are on)");
        }
        PatchArchive archive;
        try {
            archive = PatchArchive.parse(bytes);
        } catch (dev.paperswap.runtime.livepatch.PatchException e) {
            out.println("NOT APPLICABLE: " + e.getMessage());
            return 1;
        }
        PatchManifest m = archive.manifest();
        String build = BuildIdentity.of(serverJar);
        String minecraft = minecraftOf(serverJar);
        out.println("  patch " + m.id() + " for " + m.targetBuildId() + " (Minecraft " + m.targetMinecraft() + "), "
                + m.classes().size() + " class(es)");
        if (!m.targetBuildId().equals(build)) {
            problems.add("patch was built for " + m.targetBuildId() + " but the jar is " + build);
        }
        if (!m.targetMinecraft().equals(minecraft)) {
            problems.add("patch targets Minecraft " + m.targetMinecraft() + " but the jar is " + minecraft);
        }
        Map<String, String> digests = classDigests(serverJar);
        boolean structural = false;
        List<String> never = new ArrayList<>();
        for (PatchManifest.Entry e : m.classes()) {
            if (e.isNew()) {
                if (digests.containsKey(e.name())) {
                    problems.add(e.name() + " is marked as new but exists in the server jar");
                } else {
                    out.println("  new      " + e.name());
                }
                continue;
            }
            String running = digests.get(e.name());
            if (running == null) {
                problems.add(e.name() + " does not exist in the server jar");
                continue;
            }
            if (!running.equals(e.baseSha256())) {
                problems.add(e.name() + ": patch was built against " + e.baseSha256().substring(0, 12) + ", the jar has "
                        + running.substring(0, 12) + " (build the patch against this jar: -PserverJar)");
                continue;
            }
            List<ClassChange> changes;
            try {
                changes = ClassDiff.compare(ClassFileReader.read(readEntry(serverJar, e.name())), archive.infos().get(e.name()));
            } catch (ClassFormatException ex) {
                problems.add(e.name() + ": " + ex.getMessage());
                continue;
            }
            out.println("  changed  " + e.name());
            for (ClassChange c : changes) {
                if (c.kind().required().isEmpty()) {
                    never.add(e.name() + ": " + c);
                }
                structural |= !c.kind().bodyOnly();
                out.println("           " + (c.kind().bodyOnly() ? "" : "[structural] ") + c);
            }
        }
        if (!problems.isEmpty()) {
            out.println("NOT APPLICABLE to this server jar:");
            problems.forEach(p -> out.println("  - " + p));
            return 1;
        }
        if (!never.isEmpty()) {
            out.println("NOT APPLICABLE live (PaperSwap never applies these changes; restart with a rebuilt jar):");
            never.forEach(p -> out.println("  - " + p));
            return 1;
        }
        if (structural) {
            out.println("APPLICABLE ONLY with enhanced class redefinition (JetBrains Runtime with"
                    + " -XX:+AllowEnhancedClassRedefinition); a standard JVM refuses it before changing anything");
            return 3;
        }
        out.println("APPLICABLE on a standard JVM (method bodies only). The server checks everything again before applying.");
        return 0;
    }

    /**
     * Compares a class of the running jar with the same class of the reference jar.
     *
     * <p>Equivalent unless the members, the hierarchy or an instruction differ. One difference
     * is accepted: a field or method reference that names a supertype in one jar and a subtype
     * in the other (javac names the receiver's type, a remapper the declaring class), provided
     * no class between the two declares that member, so both resolve to the same member.</p>
     *
     * @return {@code null} if equivalent, else the first real difference
     */
    static String drift(byte[] base, byte[] reference, Hierarchy hierarchy) {
        try {
            ClassInfo b = ClassFileReader.read(base);
            ClassInfo r = ClassFileReader.read(reference);
            for (ClassChange c : ClassDiff.compare(b, r)) {
                if (c.kind() != ChangeKind.METHOD_BODY && c.kind() != ChangeKind.CONSTRUCTOR_BODY
                        && c.kind() != ChangeKind.STATIC_INITIALIZER) {
                    return c.toString();
                }
            }
            for (ClassInfo.MethodInfo m : b.methods()) {
                ClassInfo.MethodInfo other = r.method(m.name(), m.descriptor()).orElse(null);
                if (other == null || java.util.Objects.equals(m.codeDigest(), other.codeDigest())) {
                    continue;
                }
                List<String> lb = ClassFileReader.codeListing(base, m.name(), m.descriptor());
                List<String> lr = ClassFileReader.codeListing(reference, m.name(), m.descriptor());
                if (lb.size() != lr.size()) {
                    return m.name() + m.descriptor() + ": " + lb.size() + " vs " + lr.size() + " instructions";
                }
                for (int i = 0; i < lb.size(); i++) {
                    if (!lb.get(i).equals(lr.get(i)) && !sameMember(lb.get(i), lr.get(i), hierarchy)) {
                        return m.name() + m.descriptor() + ": '" + lb.get(i) + "' vs '" + lr.get(i) + "'";
                    }
                }
            }
            return null;
        } catch (ClassFormatException | IOException | RuntimeException e) {
            return "cannot compare: " + e;
        }
    }

    private static final java.util.regex.Pattern MEMBER_REF = java.util.regex.Pattern.compile("^(#\\d+ \\S+ )(F|M|IM):([^.]+)\\.(.+)$");

    /** Same instruction, same member name and type, owners in a sub/supertype relation without a closer declaration. */
    private static boolean sameMember(String a, String b, Hierarchy hierarchy) throws IOException {
        java.util.regex.Matcher x = MEMBER_REF.matcher(a);
        java.util.regex.Matcher y = MEMBER_REF.matcher(b);
        if (!x.matches() || !y.matches() || !x.group(1).equals(y.group(1)) || !x.group(2).equals(y.group(2))
                || !x.group(4).equals(y.group(4))) {
            return false;
        }
        boolean field = x.group(2).equals("F");
        return hierarchy.resolvesTo(x.group(3), y.group(3), field, x.group(4))
                || hierarchy.resolvesTo(y.group(3), x.group(3), field, x.group(4));
    }

    /** Super types and members of the classes in a jar, read on demand (internal names). */
    static final class Hierarchy {
        private final Path jar;
        private final Map<String, java.util.Optional<ClassInfo>> cache = new HashMap<>();

        Hierarchy(Path jar) {
            this.jar = jar;
        }

        private java.util.Optional<ClassInfo> info(String internalName) throws IOException {
            java.util.Optional<ClassInfo> known = this.cache.get(internalName);
            if (known != null) {
                return known;
            }
            java.util.Optional<ClassInfo> found = java.util.Optional.empty();
            try (JarFile file = new JarFile(this.jar.toFile(), false)) {
                JarEntry e = file.getJarEntry(internalName + ".class");
                if (e != null) {
                    try (InputStream in = file.getInputStream(e)) {
                        found = java.util.Optional.of(ClassFileReader.read(in.readAllBytes()));
                    } catch (ClassFormatException ex) {
                        found = java.util.Optional.empty();
                    }
                }
            }
            this.cache.put(internalName, found);
            return found;
        }

        /**
         * @return whether a reference to {@code memberAndType} through {@code sub} resolves to the
         * member as declared in or inherited through {@code sup}: {@code sup} is a super type of
         * {@code sub}, and no class or interface on the way (including {@code sub}) declares it
         */
        boolean resolvesTo(String sub, String sup, boolean field, String memberAndType) throws IOException {
            int colon = memberAndType.indexOf(':');
            String memberName = memberAndType.substring(0, colon);
            String type = memberAndType.substring(colon + 1);
            java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>(List.of(sub));
            java.util.Set<String> seen = new java.util.HashSet<>();
            while (!queue.isEmpty()) {
                String c = queue.poll();
                if (!seen.add(c)) {
                    continue;
                }
                if (c.equals(sup)) {
                    return true;
                }
                java.util.Optional<ClassInfo> info = info(c);
                if (info.isEmpty()) {
                    continue; // outside the jar (JDK): cannot be the declaring server class we look for
                }
                ClassInfo ci = info.get();
                boolean declares = field
                        ? ci.fields().stream().anyMatch(f -> f.name().equals(memberName) && f.descriptor().equals(type))
                        : ci.methods().stream().anyMatch(mm -> mm.name().equals(memberName) && mm.descriptor().equals(type));
                if (declares) {
                    return false; // resolved before reaching sup: different members
                }
                if (ci.superName() != null) {
                    queue.add(ci.superName());
                }
                queue.addAll(ci.interfaces());
            }
            return false;
        }
    }

    /** Prints where the bytecode of --base and --reference differs, method by method (diagnostics). */
    private static void printDrift(PrintStream out, String name, byte[] base, byte[] reference) {
        try {
            ClassInfo b = ClassFileReader.read(base);
            ClassInfo r = ClassFileReader.read(reference);
            for (ClassInfo.MethodInfo m : b.methods()) {
                ClassInfo.MethodInfo other = r.method(m.name(), m.descriptor()).orElse(null);
                if (other == null || java.util.Objects.equals(m.codeDigest(), other.codeDigest())) {
                    continue;
                }
                List<String> lb = ClassFileReader.codeListing(base, m.name(), m.descriptor());
                List<String> lr = ClassFileReader.codeListing(reference, m.name(), m.descriptor());
                int first = 0;
                while (first < lb.size() && first < lr.size() && lb.get(first).equals(lr.get(first))) {
                    first++;
                }
                out.println("  " + name + "." + m.name() + m.descriptor() + ": " + lb.size() + " lines in --base, " + lr.size()
                        + " in --reference, first difference at line " + first);
                for (int i = Math.max(0, first - 3); i < Math.min(Math.max(lb.size(), lr.size()), first + 12); i++) {
                    String x = i < lb.size() ? lb.get(i) : "";
                    String y = i < lr.size() ? lr.get(i) : "";
                    out.println((x.equals(y) ? "      = " : "      base: ") + x + (x.equals(y) ? "" : "\n      ref:  " + y));
                }
            }
        } catch (ClassFormatException | RuntimeException e) {
            out.println("  (no listing: " + e + ")");
        }
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
