package dev.lemonpvp.runtime.testsupport;

import dev.lemonpvp.runtime.security.Hashing;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Builds real module jars for tests: compiles Java sources with the JDK compiler against
 * the test class path and packs them together with a descriptor.
 */
public final class ModuleJarBuilder {

    private final Map<String, String> sources = new LinkedHashMap<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private String descriptor;
    private final List<Path> extraClasspath = new ArrayList<>();

    public static ModuleJarBuilder create() {
        return new ModuleJarBuilder();
    }

    /** Standard descriptor with the given id/version/main and extra YAML lines. */
    public ModuleJarBuilder descriptor(String id, String version, String main, String... extraLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("id: ").append(id).append('\n');
        sb.append("version: ").append(version).append('\n');
        sb.append("name: ").append(id).append(" test module\n");
        sb.append("main: ").append(main).append('\n');
        sb.append("api-version: 1\n");
        boolean hasMc = false;
        for (String l : extraLines) {
            if (l.startsWith("minecraft-versions")) {
                hasMc = true;
            }
        }
        if (!hasMc) {
            sb.append("minecraft-versions: [\"*\"]\n");
        }
        for (String l : extraLines) {
            sb.append(l).append('\n');
        }
        this.descriptor = sb.toString();
        return this;
    }

    public ModuleJarBuilder rawDescriptor(String yaml) {
        this.descriptor = yaml;
        return this;
    }

    public ModuleJarBuilder noDescriptor() {
        this.descriptor = null;
        return this;
    }

    public ModuleJarBuilder source(String className, String code) {
        this.sources.put(className, code);
        return this;
    }

    /** Adds a jar (e.g. a dependency module) to the compile class path. */
    public ModuleJarBuilder compileAgainst(Path jar) {
        this.extraClasspath.add(jar);
        return this;
    }

    public ModuleJarBuilder resource(String name, String content) {
        this.resources.put(name, content.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * Writes the jar and its {@code .sha256} sidecar.
     *
     * @return jar path
     */
    public Path writeTo(Path dir, String fileName) throws IOException {
        return writeTo(dir, fileName, true);
    }

    public Path writeTo(Path dir, String fileName, boolean sidecar) throws IOException {
        Files.createDirectories(dir);
        Path classes = Files.createTempDirectory("module-classes");
        compile(classes);
        Path jar = dir.resolve(fileName);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            if (this.descriptor != null) {
                put(out, "lemon-module.yml", this.descriptor.getBytes(StandardCharsets.UTF_8));
            }
            for (Map.Entry<String, byte[]> r : this.resources.entrySet()) {
                put(out, r.getKey(), r.getValue());
            }
            try (Stream<Path> walk = Files.walk(classes)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    put(out, classes.relativize(p).toString().replace('\\', '/'), Files.readAllBytes(p));
                }
            }
        }
        if (sidecar) {
            writeSidecar(jar);
        }
        return jar;
    }

    public static void writeSidecar(Path jar) throws IOException {
        Files.writeString(jar.resolveSibling(jar.getFileName() + ".sha256"),
                Hashing.sha256(jar) + "  " + jar.getFileName() + "\n");
    }

    private static void put(JarOutputStream out, String name, byte[] data) throws IOException {
        out.putNextEntry(new JarEntry(name));
        try (OutputStream o = new NonClosing(out)) {
            o.write(data);
        }
        out.closeEntry();
    }

    private void compile(Path outDir) throws IOException {
        if (this.sources.isEmpty()) {
            return;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("tests require a JDK (javax.tools compiler not available)");
        }
        Path srcDir = Files.createTempDirectory("module-src");
        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));
        for (Path p : this.extraClasspath) {
            cp.append(java.io.File.pathSeparator).append(p);
        }
        List<String> args = new ArrayList<>(List.of("-d", outDir.toString(), "-classpath", cp.toString(),
                "--release", "21", "-proc:none", "-nowarn"));
        for (Map.Entry<String, String> e : this.sources.entrySet()) {
            Path f = srcDir.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue());
            args.add(f.toString());
        }
        int rc = compiler.run(null, null, System.err, args.toArray(String[]::new));
        if (rc != 0) {
            throw new IllegalStateException("compilation of test module failed");
        }
    }

    private static final class NonClosing extends java.io.FilterOutputStream {
        NonClosing(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
