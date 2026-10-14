package dev.lemonpvp.runtime.plugins;

import dev.lemonpvp.runtime.security.PathGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The parts of {@code plugin.yml} / {@code paper-plugin.yml} that matter for live updates.
 *
 * @param name           plugin name
 * @param version        plugin version
 * @param kind           Bukkit or Paper plugin
 * @param main           main class
 * @param apiVersion     declared api-version, or {@code ""}
 * @param depend         required plugins
 * @param softDepend     optional plugins
 * @param loadBefore     plugins this plugin loads before
 * @param provides       alternative names this plugin provides
 * @param loadAtStartup  {@code load: STARTUP} (before worlds are loaded)
 * @param hasLibraries   declares Maven libraries the server downloads on load
 * @param hasBootstrap   Paper plugin with a bootstrapper or loader
 */
public record PluginInfo(String name, String version, Kind kind, String main, String apiVersion, List<String> depend,
                         List<String> softDepend, List<String> loadBefore, List<String> provides, boolean loadAtStartup,
                         boolean hasLibraries, boolean hasBootstrap) {

    /** Descriptor type. */
    public enum Kind {
        /** {@code plugin.yml}. */
        BUKKIT,
        /** {@code paper-plugin.yml}. */
        PAPER
    }

    public PluginInfo {
        depend = List.copyOf(depend);
        softDepend = List.copyOf(softDepend);
        loadBefore = List.copyOf(loadBefore);
        provides = List.copyOf(provides);
    }

    /**
     * @param other plugin name
     * @return whether this plugin is or provides {@code other} (case-insensitive)
     */
    public boolean answersTo(String other) {
        if (this.name.equalsIgnoreCase(other)) {
            return true;
        }
        return this.provides.stream().anyMatch(p -> p.equalsIgnoreCase(other));
    }

    /**
     * Parses a descriptor.
     *
     * @param raw  YAML content
     * @param kind which file it came from
     * @return the info
     * @throws IllegalArgumentException if name, version or main is missing
     */
    public static PluginInfo parse(Map<String, Object> raw, Kind kind) {
        String name = text(raw.get("name"));
        String version = text(raw.get("version"));
        String main = text(raw.get("main"));
        List<String> missing = new ArrayList<>();
        if (name.isEmpty()) {
            missing.add("name");
        }
        if (version.isEmpty()) {
            missing.add("version");
        }
        if (main.isEmpty()) {
            missing.add("main");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException((kind == Kind.PAPER ? "paper-plugin.yml" : "plugin.yml") + " lacks " + missing);
        }
        List<String> depend = new ArrayList<>();
        List<String> soft = new ArrayList<>();
        List<String> before = new ArrayList<>();
        boolean bootstrap = false;
        if (kind == Kind.BUKKIT) {
            depend.addAll(list(raw.get("depend")));
            soft.addAll(list(raw.get("softdepend")));
            before.addAll(list(raw.get("loadbefore")));
        } else {
            bootstrap = !text(raw.get("bootstrapper")).isEmpty() || !text(raw.get("loader")).isEmpty();
            paperDependencies(raw.get("dependencies"), depend, soft, before);
        }
        boolean startup = "STARTUP".equals(text(raw.get("load")).toUpperCase(Locale.ROOT));
        boolean libraries = !list(raw.get("libraries")).isEmpty();
        return new PluginInfo(name, version, kind, main, text(raw.get("api-version")), depend, soft, before,
                list(raw.get("provides")), startup, libraries, bootstrap);
    }

    /** Paper: {@code dependencies: {server: {Name: {required: true, load: BEFORE|AFTER|OMIT}}}}. */
    private static void paperDependencies(Object value, List<String> depend, List<String> soft, List<String> before) {
        if (!(value instanceof Map<?, ?> deps)) {
            return;
        }
        Object server = deps.get("server");
        if (!(server instanceof Map<?, ?> serverDeps)) {
            return;
        }
        for (Map.Entry<?, ?> e : serverDeps.entrySet()) {
            String dep = String.valueOf(e.getKey());
            boolean required = true;
            String load = "OMIT";
            if (e.getValue() instanceof Map<?, ?> cfg) {
                required = !Boolean.FALSE.equals(cfg.get("required"));
                load = text(cfg.get("load")).toUpperCase(Locale.ROOT);
            }
            (required ? depend : soft).add(dep);
            if ("AFTER".equals(load)) {
                before.add(dep);
            }
        }
    }

    private static String text(Object o) {
        return o == null ? "" : PathGuard.sanitize(String.valueOf(o)).strip();
    }

    private static List<String> list(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                if (x != null) {
                    out.add(text(x));
                }
            }
        } else if (o instanceof String s && !s.isBlank()) {
            out.add(text(s));
        }
        return out;
    }
}
