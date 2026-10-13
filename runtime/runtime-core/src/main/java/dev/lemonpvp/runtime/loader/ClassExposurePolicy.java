package dev.lemonpvp.runtime.loader;

import java.util.List;

/**
 * Decides which server classes a module class loader may resolve.
 *
 * <p>This is an API-hygiene measure, <strong>not</strong> a security sandbox: module code
 * can always reach any class through reflection on objects it receives from the server.
 * It keeps modules on the supported API surface (runtime API, Paper/Bukkit API and
 * libraries) and makes accidental dependencies on internals fail early and loudly.</p>
 */
public final class ClassExposurePolicy {

    private static final String RUNTIME_PREFIX = "dev.lemonpvp.runtime.";
    private static final String RUNTIME_API_PREFIX = "dev.lemonpvp.runtime.api.";
    private static final List<String> SERVER_INTERNAL_PREFIXES = List.of("net.minecraft.", "org.bukkit.craftbukkit.");

    private final boolean allowServerInternals;

    /** @param allowServerInternals whether {@code net.minecraft} and CraftBukkit classes are visible */
    public ClassExposurePolicy(boolean allowServerInternals) {
        this.allowServerInternals = allowServerInternals;
    }

    /**
     * @param className binary class name
     * @return {@code null} if the class may be loaded from the parent, otherwise the reason it is hidden
     */
    public String hiddenReason(String className) {
        if (className.startsWith(RUNTIME_PREFIX) && !className.startsWith(RUNTIME_API_PREFIX)) {
            return "runtime internals are not part of the module API";
        }
        if (!this.allowServerInternals) {
            for (String prefix : SERVER_INTERNAL_PREFIXES) {
                if (className.startsWith(prefix)) {
                    return "server internals are hidden (allow-server-internals: false)";
                }
            }
        }
        return null;
    }

    /**
     * @param resourceName resource path such as {@code net/minecraft/Foo.class}
     * @return whether the resource may be served from the parent loader
     */
    public boolean isResourceVisible(String resourceName) {
        if (!resourceName.endsWith(".class")) {
            return true;
        }
        String cls = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
        return hiddenReason(cls) == null;
    }

    /** @return whether server internals are visible */
    public boolean allowServerInternals() {
        return this.allowServerInternals;
    }
}
