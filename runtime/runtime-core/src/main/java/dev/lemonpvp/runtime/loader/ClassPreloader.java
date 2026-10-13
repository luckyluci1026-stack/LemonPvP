package dev.lemonpvp.runtime.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Defines all classes of a module jar on a worker thread without initializing them.
 *
 * <p>Reading and defining classes involves file I/O and bytecode parsing. Doing it ahead
 * of time keeps that work off the server main thread; static initializers still run later,
 * on the main thread, when the module is instantiated.</p>
 */
public final class ClassPreloader {

    private static final int MAX_WARNINGS = 20;

    private ClassPreloader() {
    }

    /**
     * Result of preloading.
     *
     * @param defined  number of classes defined
     * @param warnings classes that could not be defined (e.g. missing optional dependencies)
     */
    public record Result(int defined, List<String> warnings) {
        public Result {
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * @param loader module class loader
     * @param jar    staged jar the loader reads from
     * @return the result
     * @throws IOException if the jar cannot be read
     */
    public static Result preload(ModuleClassLoader loader, Path jar) throws IOException {
        int defined = 0;
        List<String> warnings = new ArrayList<>();
        try (JarFile file = new JarFile(jar.toFile(), false)) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String n = e.getName();
                if (e.isDirectory() || !n.endsWith(".class") || n.startsWith("META-INF/") || n.endsWith("module-info.class")
                        || n.endsWith("package-info.class")) {
                    continue;
                }
                String className = n.substring(0, n.length() - 6).replace('/', '.');
                try {
                    Class.forName(className, false, loader);
                    defined++;
                } catch (ClassNotFoundException | LinkageError ex) {
                    if (warnings.size() < MAX_WARNINGS) {
                        warnings.add(className + ": " + ex);
                    }
                }
            }
        }
        return new Result(defined, warnings);
    }
}
