package dev.paperswap.runtime.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Class loader of one module instance.
 *
 * <p>Lookup order for classes:</p>
 * <ol>
 *     <li>{@link ClassExposurePolicy} rejects hidden packages (runtime internals and, by
 *     default, server internals).</li>
 *     <li>Parent (server class loader): JDK, Paper/Bukkit API, bundled libraries and the
 *     runtime API. Parent-first guarantees that API types are shared with the server; a
 *     library bundled by a module is ignored if the server already ships it.</li>
 *     <li>The module's own jar.</li>
 *     <li>Jars of declared module dependencies (only their own classes, not transitively).</li>
 *     <li>Class loaders of declared plugin dependencies.</li>
 * </ol>
 * <p>Resources are looked up in the module jar first so that a module always reads its
 * own {@code config.yml} and not an equally named server resource.</p>
 *
 * <p>Closing the loader closes the jar and refuses further class loading. It does
 * <em>not</em> remove already loaded classes from the JVM; they are garbage collected only
 * once nothing references the loader, its classes or their instances any more.</p>
 */
public final class ModuleClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final String moduleId;
    private final ClassExposurePolicy policy;
    private final List<ModuleClassLoader> dependencies;
    private final List<ClassLoader> pluginLoaders;
    private volatile boolean closed;

    /**
     * @param moduleId      module id (used in names and messages)
     * @param jar           staged module jar
     * @param parent        server class loader
     * @param dependencies  class loaders of module dependencies
     * @param pluginLoaders class loaders of plugin dependencies
     * @param policy        exposure policy
     * @throws IOException if the jar path cannot be converted to a URL
     */
    public ModuleClassLoader(String moduleId, Path jar, ClassLoader parent, List<ModuleClassLoader> dependencies,
                             List<ClassLoader> pluginLoaders, ClassExposurePolicy policy) throws IOException {
        super("paperswap-module-" + moduleId, new URL[]{jar.toUri().toURL()}, parent);
        this.moduleId = moduleId;
        this.policy = policy;
        this.dependencies = List.copyOf(dependencies);
        this.pluginLoaders = List.copyOf(pluginLoaders);
    }

    /** @return id of the owning module */
    public String moduleId() {
        return this.moduleId;
    }

    /** @return whether {@link #close()} was called */
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = loadNotYetLoaded(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private Class<?> loadNotYetLoaded(String name) throws ClassNotFoundException {
        if (this.closed) {
            throw new ClassNotFoundException(name + " (class loader of module '" + this.moduleId + "' is closed)");
        }
        String hidden = this.policy.hiddenReason(name);
        if (hidden != null) {
            throw new ClassNotFoundException(name + " (" + hidden + ")");
        }
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException ignored) {
            // not provided by the server
        }
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            // not in this module
        }
        for (ModuleClassLoader dep : this.dependencies) {
            Class<?> c = dep.findOwnClass(name);
            if (c != null) {
                return c;
            }
        }
        for (ClassLoader plugin : this.pluginLoaders) {
            try {
                return plugin.loadClass(name);
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Looks up a class defined by this module's own jar (used by dependent modules).
     *
     * @param name binary class name
     * @return the class or {@code null}
     */
    Class<?> findOwnClass(String name) {
        if (this.closed || this.policy.hiddenReason(name) != null) {
            return null;
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c.getClassLoader() == this ? c : null;
            }
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    @Override
    public URL getResource(String name) {
        if (this.closed) {
            return null;
        }
        URL own = findResource(name);
        if (own != null) {
            return own;
        }
        if (this.policy.isResourceVisible(name)) {
            URL parent = getParent().getResource(name);
            if (parent != null) {
                return parent;
            }
        }
        for (ModuleClassLoader dep : this.dependencies) {
            URL u = dep.isClosed() ? null : dep.findResource(name);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> all = new ArrayList<>();
        if (this.closed) {
            return Collections.emptyEnumeration();
        }
        all.addAll(Collections.list(findResources(name)));
        if (this.policy.isResourceVisible(name)) {
            all.addAll(Collections.list(getParent().getResources(name)));
        }
        return Collections.enumeration(all);
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        super.close();
    }

    @Override
    public String toString() {
        return "ModuleClassLoader[" + this.moduleId + (this.closed ? ", closed" : "") + "]";
    }
}
