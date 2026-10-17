package dev.lemonpvp.runtime.livepatch;

import dev.lemonpvp.runtime.livepatch.classfile.ClassFileReader;
import dev.lemonpvp.runtime.livepatch.classfile.ClassFormatException;
import dev.lemonpvp.runtime.livepatch.classfile.ClassInfo;
import dev.lemonpvp.runtime.loader.ModuleClassLoader;
import dev.lemonpvp.runtime.security.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Searches and describes the classes the JVM has loaded
 * ({@link Instrumentation#getAllLoadedClasses()}). Read-only: nothing is loaded, initialized
 * or changed, except by {@link #runningBytecode}, which is a retransformation on request.
 */
public final class ClassCatalog {

    /** Rows per page of a search. */
    public static final int PAGE_SIZE = 12;
    /** Longest accepted filter. */
    public static final int MAX_FILTER = 200;

    private ClassCatalog() {
    }

    /** Which part of the server a class belongs to. */
    public enum Component {
        /** Java runtime (bootstrap or platform class loader). */
        JDK("jdk"),
        /** {@code net.minecraft.*}, {@code com.mojang.*}. */
        MINECRAFT("minecraft"),
        /** {@code org.bukkit.craftbukkit.*}. */
        CRAFTBUKKIT("craftbukkit"),
        /** Paper and Spigot ({@code io.papermc.paper}, {@code com.destroystokyo.paper}, {@code org.spigotmc}, {@code ca.spottedleaf}). */
        PAPER("paper"),
        /** Bukkit API ({@code org.bukkit.*} outside CraftBukkit). */
        BUKKIT("bukkit"),
        /** Lemon runtime and Lemon server code ({@code dev.lemonpvp.*} in the server class loader). */
        LEMON("lemon"),
        /** Lemon runtime module (module class loader). */
        MODULE("module"),
        /** Paper/Bukkit plugin (plugin class loader). */
        PLUGIN("plugin"),
        /** The live-patch agent. */
        AGENT("agent"),
        /** Other classes of the server class loader (bundled libraries: Netty, Adventure, Guava, ...). */
        LIBRARY("library"),
        /** Everything else (launcher, other agents, other class loaders). */
        OTHER("other");

        private final String id;

        Component(String id) {
            this.id = id;
        }

        /** @return name used in commands */
        public String id() {
            return this.id;
        }

        /**
         * @param text command argument
         * @return the component, if {@code text} names one
         */
        public static Optional<Component> parse(String text) {
            String t = text.toLowerCase(Locale.ROOT);
            for (Component c : values()) {
                if (c.id.equals(t)) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * What the caller knows about class loaders.
     *
     * @param serverLoader  the server class loader
     * @param pluginLoaders plugin name per plugin class loader (identity map)
     */
    public record Loaders(ClassLoader serverLoader, Map<ClassLoader, String> pluginLoaders) {
    }

    /**
     * Classification of one class.
     *
     * @param component part of the server
     * @param owner     plugin name or module id, or {@code null}
     */
    public record Origin(Component component, String owner) {
        /** @return e.g. {@code plugin MyPlugin} or {@code paper} */
        public String label() {
            return this.owner == null ? this.component.id() : this.component.id() + " " + this.owner;
        }
    }

    /**
     * @param type    class
     * @param loaders loader knowledge
     * @return where the class belongs
     */
    public static Origin classify(Class<?> type, Loaders loaders) {
        ClassLoader loader = type.getClassLoader();
        if (loader != null) {
            String plugin = loaders.pluginLoaders().get(loader);
            if (plugin != null) {
                return new Origin(Component.PLUGIN, plugin);
            }
            if (loader instanceof ModuleClassLoader m) {
                return new Origin(Component.MODULE, m.moduleId());
            }
        }
        return new Origin(classify(type.getName(), loader == null || loader == ClassLoader.getPlatformClassLoader(),
                loader != null && loader == loaders.serverLoader()), null);
    }

    /**
     * Classification by name and loader kind, for classes of no plugin or module.
     *
     * @param name         binary name
     * @param jdkLoader    loaded by the bootstrap or platform class loader
     * @param serverLoader loaded by the server class loader
     * @return the component
     */
    static Component classify(String name, boolean jdkLoader, boolean serverLoader) {
        if (name.startsWith("dev.lemonpvp.livepatch.agent.")) {
            return Component.AGENT;
        }
        if (jdkLoader) {
            return Component.JDK;
        }
        if (name.startsWith("net.minecraft.") || name.startsWith("com.mojang.")) {
            return Component.MINECRAFT;
        }
        if (name.startsWith("org.bukkit.craftbukkit.")) {
            return Component.CRAFTBUKKIT;
        }
        if (name.startsWith("io.papermc.paper.") || name.startsWith("com.destroystokyo.paper.") || name.startsWith("org.spigotmc.")
                || name.startsWith("ca.spottedleaf.")) {
            return Component.PAPER;
        }
        if (name.startsWith("org.bukkit.")) {
            return Component.BUKKIT;
        }
        if (name.startsWith("dev.lemonpvp.") && serverLoader) {
            return Component.LEMON;
        }
        return serverLoader ? Component.LIBRARY : Component.OTHER;
    }

    /**
     * A search request.
     *
     * @param filter    {@code *} for all, a text contained in the binary name (case-insensitive), or a pattern with
     *                  {@code *} wildcards matching the whole name
     * @param component only this component, or {@code null}
     * @param page      1-based page
     */
    public record Query(String filter, Component component, int page) {
        /**
         * Parses {@code <filter> [component] [page]}.
         *
         * @param args arguments after {@code classes}
         * @return the query
         * @throws IllegalArgumentException with a usage message
         */
        public static Query parse(List<String> args) {
            if (args.isEmpty() || args.size() > 3) {
                throw new IllegalArgumentException("expected <filter> [component] [page]");
            }
            String filter = args.get(0);
            if (filter.length() > MAX_FILTER || !filter.matches("[A-Za-z0-9_$.*\\-]+")) {
                throw new IllegalArgumentException("filter must be a class or package name (letters, digits, . _ $ - and *)");
            }
            Component component = null;
            int page = 1;
            for (String a : args.subList(1, args.size())) {
                if (a.matches("[0-9]{1,6}")) {
                    page = Math.max(1, Integer.parseInt(a));
                } else {
                    final String text = a;
                    component = Component.parse(a).orElseThrow(() -> new IllegalArgumentException("unknown component '" + text
                            + "'; one of " + java.util.Arrays.stream(Component.values()).map(Component::id).toList()));
                }
            }
            return new Query(filter, component, page);
        }

        Predicate<String> nameMatcher() {
            if (this.filter.equals("*")) {
                return n -> true;
            }
            if (this.filter.contains("*")) {
                StringBuilder regex = new StringBuilder();
                for (String part : this.filter.split("\\*", -1)) {
                    if (!regex.isEmpty()) {
                        regex.append(".*");
                    }
                    regex.append(Pattern.quote(part));
                }
                Pattern p = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
                return n -> p.matcher(n).matches();
            }
            String needle = this.filter.toLowerCase(Locale.ROOT);
            return n -> n.toLowerCase(Locale.ROOT).contains(needle);
        }
    }

    /**
     * One search result.
     *
     * @param name       binary name
     * @param origin     component and owner
     * @param loader     defining class loader
     * @param modifiable {@link Instrumentation#isModifiableClass}
     * @param hidden     hidden class (lambda proxies and the like)
     * @param lemon      Lemon's change of this class, or {@code null}
     */
    public record Row(String name, Origin origin, String loader, boolean modifiable, boolean hidden, String lemon) {
    }

    /**
     * A page of search results.
     *
     * @param query   the query
     * @param rows    rows of this page
     * @param matches number of matching classes
     * @param loaded  number of loaded classes (without array classes)
     * @param pages   number of pages
     * @param micros  time the search took
     */
    public record Page(Query query, List<Row> rows, int matches, int loaded, int pages, long micros) {
        public Page {
            rows = List.copyOf(rows);
        }
    }

    /**
     * @param inst     instrumentation
     * @param query    query
     * @param loaders  loader knowledge
     * @param registry Lemon's patch registry
     * @param patches  Lemon's transformer
     * @return the requested page; {@code isModifiableClass} is only asked for the rows shown
     */
    public static Page search(Instrumentation inst, Query query, Loaders loaders, LivePatchRegistry registry,
                              PatchTransformer patches) {
        long start = System.nanoTime();
        Predicate<String> matcher = query.nameMatcher();
        List<Class<?>> matches = new ArrayList<>();
        int loaded = 0;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.isArray() || c.isPrimitive()) {
                continue;
            }
            loaded++;
            if (!matcher.test(c.getName())) {
                continue;
            }
            if (query.component() != null && classify(c, loaders).component() != query.component()) {
                continue;
            }
            matches.add(c);
        }
        matches.sort(Comparator.comparing((Class<?> c) -> c.getName()).thenComparing(c -> PatchTransformer.loaderName(c.getClassLoader())));
        int pages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(query.page(), pages);
        List<Row> rows = new ArrayList<>();
        for (Class<?> c : matches.subList(Math.min(matches.size(), (page - 1) * PAGE_SIZE), Math.min(matches.size(), page * PAGE_SIZE))) {
            rows.add(new Row(c.getName(), classify(c, loaders), PatchTransformer.loaderName(c.getClassLoader()), inst.isModifiableClass(c),
                    c.isHidden(), lemonState(c, loaders, registry, patches)));
        }
        return new Page(new Query(query.filter(), query.component(), page), rows, matches.size(), loaded, pages,
                (System.nanoTime() - start) / 1000);
    }

    /**
     * @return what Lemon changed in the class, or {@code null}
     */
    static String lemonState(Class<?> c, Loaders loaders, LivePatchRegistry registry, PatchTransformer patches) {
        Optional<PatchTransformer.SlotInfo> slot = patches.slot(c);
        if (slot.isPresent()) {
            return "retransformed by " + String.join(" + ", slot.get().patchIds())
                    + (slot.get().displaced() == null ? "" : " (DISPLACED)");
        }
        if (c.getClassLoader() != null && c.getClassLoader() == loaders.serverLoader()) {
            return registry.current(c.getName()).map(v -> "redefined by " + v.installedBy()).orElse(null);
        }
        return null;
    }

    /**
     * @param name binary class name from a command
     * @return why it is not a valid binary class name, or {@code null}
     */
    public static String nameProblem(String name) {
        if (name.isEmpty() || name.length() > 300 || !name.matches("[A-Za-z0-9_$]+(\\.[A-Za-z0-9_$]+)*")) {
            return "'" + name.replaceAll("[^A-Za-z0-9_$.]", "?") + "' is not a binary class name such as"
                    + " org.spigotmc.TicksPerSecondCommand (nested classes: Outer$Inner)";
        }
        return null;
    }

    /**
     * @param inst instrumentation
     * @param name binary name
     * @return loaded classes with exactly that name (several class loaders may define one)
     */
    public static List<Class<?>> find(Instrumentation inst, String name) {
        List<Class<?>> out = new ArrayList<>();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (!c.isArray() && c.getName().equals(name)) {
                out.add(c);
            }
        }
        out.sort(Comparator.comparing(c -> PatchTransformer.loaderName(c.getClassLoader())));
        return out;
    }

    /**
     * Description of one loaded class.
     *
     * @param name            binary name
     * @param kind            class, interface, enum, record or annotation (and hidden/synthetic)
     * @param origin          component and owner
     * @param loader          defining class loader
     * @param module          JPMS module
     * @param codeSource      code source location as reported by the defining class loader, or an explanation
     * @param modifiable      {@link Instrumentation#isModifiableClass}
     * @param notModifiable   why the JVM never modifies it, or {@code null}
     * @param classFile       class file found through the defining loader, or an explanation why there is none
     * @param classFileSha256 its digest (the base a patch must be built against), or {@code null}
     * @param lemon           Lemon's change, or {@code null}
     * @param transformer     transformer state, or {@code null}
     */
    public record Description(String name, String kind, Origin origin, String loader, String module, String codeSource,
                              boolean modifiable, String notModifiable, String classFile, String classFileSha256, String lemon,
                              PatchTransformer.SlotInfo transformer) {
    }

    /**
     * @param inst     instrumentation
     * @param c        loaded class
     * @param loaders  loader knowledge
     * @param registry Lemon's patch registry
     * @param patches  Lemon's transformer
     * @return the description; reads the class file through the defining loader, loads nothing
     */
    public static Description describe(Instrumentation inst, Class<?> c, Loaders loaders, LivePatchRegistry registry,
                                       PatchTransformer patches) {
        String kind = c.isAnnotation() ? "annotation" : c.isInterface() ? "interface" : c.isEnum() ? "enum"
                : c.isRecord() ? "record" : "class";
        if (c.isHidden()) {
            kind += ", hidden";
        }
        if (c.isSynthetic()) {
            kind += ", synthetic";
        }
        String module = c.getModule().isNamed() ? c.getModule().getName() : "unnamed module";
        String codeSource;
        ProtectionDomain pd = c.getProtectionDomain();
        CodeSource cs = pd == null ? null : pd.getCodeSource();
        URL location = cs == null ? null : cs.getLocation();
        codeSource = location == null ? "none reported (bootstrap/platform class, hidden class or defined without a code source)"
                : location.toString();
        boolean modifiable = inst.isModifiableClass(c);
        String notModifiable = null;
        if (!modifiable) {
            notModifiable = c.isHidden() ? "hidden classes are never modifiable"
                    : "the JVM reports it as not modifiable (for example a class of the JVM itself)";
        }
        String classFile;
        String sha = null;
        Optional<byte[]> bytes = Optional.empty();
        if (c.isHidden()) {
            classFile = "none (hidden classes have no class file)";
        } else {
            try {
                bytes = classFile(c);
                classFile = bytes.map(b -> "found through the defining class loader: " + summary(b))
                        .orElse("not found through the defining class loader");
            } catch (IOException e) {
                classFile = "cannot be read: " + e.getMessage();
            }
        }
        if (bytes.isPresent()) {
            sha = Hashing.sha256(bytes.get());
        }
        return new Description(c.getName(), kind, classify(c, loaders), PatchTransformer.loaderName(c.getClassLoader()), module,
                codeSource, modifiable, notModifiable, classFile, sha, lemonState(c, loaders, registry, patches),
                patches.slot(c).orElse(null));
    }

    private static String summary(byte[] bytes) {
        try {
            ClassInfo info = ClassFileReader.read(bytes);
            return bytes.length + " bytes, class file version " + info.majorVersion() + ", " + info.methods().size() + " methods, "
                    + info.fields().size() + " fields, super " + (info.superName() == null ? "-" : info.superName().replace('/', '.'))
                    + (info.interfaces().isEmpty() ? "" : ", " + info.interfaces().size() + " interface(s)");
        } catch (ClassFormatException e) {
            return bytes.length + " bytes (not parseable: " + e.getMessage() + ")";
        }
    }

    /**
     * Reads the class file of a class through its defining loader (bootstrap: the system
     * resources). This is the file the class was loaded from as long as the loader resolves
     * resources the same way as classes, which holds for the server, plugin and module
     * loaders.
     */
    static Optional<byte[]> classFile(Class<?> c) throws IOException {
        String path = c.getName().replace('.', '/') + ".class";
        ClassLoader loader = c.getClassLoader();
        try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(path) : loader.getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            byte[] data = in.readNBytes(PatchArchive.MAX_CLASS_BYTES + 1);
            if (data.length > PatchArchive.MAX_CLASS_BYTES) {
                throw new IOException(path + " is larger than " + PatchArchive.MAX_CLASS_BYTES + " bytes");
            }
            return Optional.of(data);
        }
    }

    /**
     * Compares the bytecode the JVM runs for a class with an expected class file. This is a
     * retransformation: the class is passed through every retransformation-capable
     * transformer (Lemon's supplies its patch again; transformers of other agents run
     * again too). Must not run concurrently with a patch operation.
     *
     * @param inst     instrumentation with retransformation support
     * @param c        modifiable class
     * @param expected expected class file
     * @return differences; empty if the running bytecode equals the expected class file
     * @throws Exception if the JVM refuses or the class file cannot be parsed
     */
    public static List<ClassChange> runningBytecode(Instrumentation inst, Class<?> c, byte[] expected) throws Exception {
        byte[] running = AppliedBytecodeCheck.readRunning(inst, List.of(c)).get(c);
        if (running == null) {
            throw new IllegalStateException("the JVM did not report the bytecode of " + c.getName());
        }
        return ClassDiff.compare(ClassFileReader.read(running), ClassFileReader.read(expected));
    }
}
