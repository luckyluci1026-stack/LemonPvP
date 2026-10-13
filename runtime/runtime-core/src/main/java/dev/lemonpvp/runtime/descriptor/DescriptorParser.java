package dev.lemonpvp.runtime.descriptor;

import dev.lemonpvp.runtime.api.Capability;
import dev.lemonpvp.runtime.api.ModuleDependency;
import dev.lemonpvp.runtime.api.ModuleDescriptor;
import dev.lemonpvp.runtime.api.PermissionDeclaration;
import dev.lemonpvp.runtime.api.PermissionDeclaration.PermissionDefault;
import dev.lemonpvp.runtime.config.YamlSupport;
import dev.lemonpvp.runtime.security.PathGuard;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses and validates {@code lemon-module.yml}. Collects all problems instead of failing
 * on the first one so that module authors get a complete report.
 */
public final class DescriptorParser {

    private static final Pattern CLASS_NAME = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern PERMISSION_NODE = Pattern.compile("[a-z0-9_-]+(\\.[a-z0-9_*-]+)+");
    private static final Pattern PLUGIN_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Set<String> KNOWN_KEYS = Set.of(
            "id", "version", "name", "description", "authors", "main", "api-version", "minecraft-versions",
            "capabilities", "permissions", "dependencies", "plugin-dependencies", "load-order", "resources");
    /** Packages a module main class must never live in. */
    private static final List<String> FORBIDDEN_MAIN_PREFIXES = List.of(
            "java.", "javax.", "jdk.", "sun.", "dev.lemonpvp.runtime.", "org.bukkit.", "io.papermc.", "net.minecraft.");
    private static final int MAX_LIST = 64;

    private DescriptorParser() {
    }

    /**
     * @param in     descriptor YAML
     * @param source name used in messages
     * @return the validated descriptor
     * @throws DescriptorException if the descriptor is invalid
     */
    public static ModuleDescriptor parse(InputStream in, String source) throws DescriptorException {
        Map<String, Object> raw;
        try {
            raw = YamlSupport.load(in, source);
        } catch (YamlSupport.YamlFormatException e) {
            throw new DescriptorException(e.getMessage());
        }
        return parse(raw);
    }

    /**
     * @param raw parsed YAML mapping
     * @return the validated descriptor
     * @throws DescriptorException if the descriptor is invalid
     */
    public static ModuleDescriptor parse(Map<String, Object> raw) throws DescriptorException {
        List<String> problems = new ArrayList<>();
        for (String key : raw.keySet()) {
            if (!KNOWN_KEYS.contains(key)) {
                problems.add("unknown key '" + PathGuard.sanitize(key) + "'");
            }
        }

        String id = string(raw, "id", problems, true);
        String idProblem = id == null ? null : ModuleIds.problem(id);
        if (idProblem != null) {
            problems.add(idProblem);
        }

        String version = string(raw, "version", problems, true);
        if (version != null && !SemanticVersion.isValid(version)) {
            problems.add("version '" + PathGuard.sanitize(version) + "' is not a semantic version (MAJOR.MINOR.PATCH)");
        }

        String name = string(raw, "name", problems, false);
        if (name != null && name.length() > 64) {
            problems.add("name is longer than 64 characters");
        }
        String description = string(raw, "description", problems, false);
        if (description != null && description.length() > 512) {
            problems.add("description is longer than 512 characters");
        }
        List<String> authors = stringList(raw, "authors", problems);

        String main = string(raw, "main", problems, true);
        if (main != null) {
            if (!CLASS_NAME.matcher(main).matches()) {
                problems.add("main '" + PathGuard.sanitize(main) + "' is not a fully qualified class name in a package");
            } else {
                for (String prefix : FORBIDDEN_MAIN_PREFIXES) {
                    if (main.startsWith(prefix)) {
                        problems.add("main class must not be in package " + prefix + "*");
                    }
                }
            }
        }

        int apiVersion = integer(raw, "api-version", problems, true, -1);
        if (raw.containsKey("api-version") && apiVersion < 1) {
            problems.add("api-version must be >= 1");
        }

        List<String> mcVersions = stringList(raw, "minecraft-versions", problems);
        if (mcVersions.isEmpty()) {
            problems.add("minecraft-versions must list at least one pattern (use \"*\" to accept every version)");
        }
        for (String p : mcVersions) {
            if (!MinecraftVersionMatcher.isValidPattern(p)) {
                problems.add("invalid minecraft version pattern '" + PathGuard.sanitize(p) + "'");
            }
        }

        Set<Capability> capabilities = EnumSet.noneOf(Capability.class);
        for (String c : stringList(raw, "capabilities", problems)) {
            Capability.byKey(c.toLowerCase(Locale.ROOT)).ifPresentOrElse(capabilities::add,
                    () -> problems.add("unknown capability '" + PathGuard.sanitize(c) + "'"));
        }

        List<PermissionDeclaration> permissions = permissions(raw, id, problems);
        List<ModuleDependency> dependencies = dependencies(raw, id, problems);

        List<String> pluginDependencies = stringList(raw, "plugin-dependencies", problems);
        for (String p : pluginDependencies) {
            if (!PLUGIN_NAME.matcher(p).matches()) {
                problems.add("invalid plugin name '" + PathGuard.sanitize(p) + "'");
            }
        }

        int loadOrder = integer(raw, "load-order", problems, false, 0);

        List<String> resources = stringList(raw, "resources", problems);
        for (String r : resources) {
            if (!PathGuard.isSafeRelativePath(r)) {
                problems.add("resource path '" + PathGuard.sanitize(r) + "' is not a safe relative path");
            }
        }
        if (new HashSet<>(resources).size() != resources.size()) {
            problems.add("resources contains duplicates");
        }

        if (!problems.isEmpty()) {
            throw new DescriptorException(problems);
        }
        return new ModuleDescriptor(id, version, name == null ? id : name, description, authors, main, apiVersion,
                mcVersions, capabilities, permissions, dependencies, pluginDependencies, loadOrder, resources);
    }

    private static String string(Map<String, Object> raw, String key, List<String> problems, boolean required) {
        Object v = raw.get(key);
        if (v == null) {
            if (required) {
                problems.add("missing required key '" + key + "'");
            }
            return null;
        }
        if (v instanceof String s) {
            return s.trim();
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        problems.add("'" + key + "' must be a string");
        return null;
    }

    private static int integer(Map<String, Object> raw, String key, List<String> problems, boolean required, int def) {
        Object v = raw.get(key);
        if (v == null) {
            if (required) {
                problems.add("missing required key '" + key + "'");
            }
            return def;
        }
        if (v instanceof Integer i) {
            return i;
        }
        problems.add("'" + key + "' must be an integer");
        return def;
    }

    private static List<String> stringList(Map<String, Object> raw, String key, List<String> problems) {
        Object v = raw.get(key);
        if (v == null) {
            return List.of();
        }
        if (v instanceof String s) {
            return List.of(s.trim());
        }
        if (!(v instanceof List<?> list)) {
            problems.add("'" + key + "' must be a list");
            return List.of();
        }
        if (list.size() > MAX_LIST) {
            problems.add("'" + key + "' has more than " + MAX_LIST + " entries");
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String || o instanceof Number) {
                out.add(String.valueOf(o).trim());
            } else {
                problems.add("'" + key + "' must only contain strings");
            }
        }
        return out;
    }

    private static List<PermissionDeclaration> permissions(Map<String, Object> raw, String id, List<String> problems) {
        Object v = raw.get("permissions");
        if (v == null) {
            return List.of();
        }
        if (!(v instanceof Map<?, ?> map)) {
            problems.add("'permissions' must be a mapping of node -> {description, default}");
            return List.of();
        }
        List<PermissionDeclaration> out = new ArrayList<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String node = String.valueOf(e.getKey());
            if (!PERMISSION_NODE.matcher(node).matches()) {
                problems.add("invalid permission node '" + PathGuard.sanitize(node) + "'");
                continue;
            }
            if (id != null && !node.startsWith(id + ".")) {
                problems.add("permission '" + node + "' must start with '" + id + ".'");
                continue;
            }
            String desc = "";
            PermissionDefault def = PermissionDefault.OP;
            if (e.getValue() instanceof Map<?, ?> props) {
                Object d = props.get("description");
                desc = d == null ? "" : String.valueOf(d);
                Object dv = props.get("default");
                if (dv != null) {
                    String s = String.valueOf(dv).toUpperCase(Locale.ROOT).replace('-', '_');
                    try {
                        def = PermissionDefault.valueOf(s);
                    } catch (IllegalArgumentException ex) {
                        problems.add("permission '" + node + "' has invalid default '" + PathGuard.sanitize(String.valueOf(dv))
                                + "' (true, false, op, not-op)");
                    }
                }
            } else if (e.getValue() != null) {
                problems.add("permission '" + node + "' must be a mapping");
            }
            out.add(new PermissionDeclaration(node, desc, def));
        }
        return out;
    }

    private static List<ModuleDependency> dependencies(Map<String, Object> raw, String id, List<String> problems) {
        Object v = raw.get("dependencies");
        if (v == null) {
            return List.of();
        }
        if (!(v instanceof List<?> list)) {
            problems.add("'dependencies' must be a list");
            return List.of();
        }
        List<ModuleDependency> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Object o : list) {
            String depId;
            String range = "*";
            boolean optional = false;
            if (o instanceof String s) {
                depId = s.trim();
            } else if (o instanceof Map<?, ?> m) {
                Object i = m.get("id");
                depId = i == null ? null : String.valueOf(i).trim();
                Object r = m.get("version");
                range = r == null ? "*" : String.valueOf(r);
                Object opt = m.get("optional");
                if (opt instanceof Boolean b) {
                    optional = b;
                } else if (opt != null) {
                    problems.add("dependency 'optional' must be true or false");
                }
            } else {
                problems.add("dependency entries must be strings or mappings");
                continue;
            }
            String depProblem = depId == null ? "dependency without id" : ModuleIds.problem(depId);
            if (depProblem != null) {
                problems.add("dependency: " + depProblem);
                continue;
            }
            if (depId.equals(id)) {
                problems.add("module must not depend on itself");
                continue;
            }
            if (!seen.add(depId)) {
                problems.add("duplicate dependency '" + depId + "'");
                continue;
            }
            try {
                VersionRange.parse(range);
            } catch (IllegalArgumentException e) {
                problems.add("dependency '" + depId + "' has invalid version range: " + PathGuard.sanitize(e.getMessage()));
                continue;
            }
            out.add(new ModuleDependency(depId, range, optional));
        }
        return out;
    }
}
