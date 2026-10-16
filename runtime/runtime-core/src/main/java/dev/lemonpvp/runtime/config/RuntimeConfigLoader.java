package dev.lemonpvp.runtime.config;

import dev.lemonpvp.runtime.descriptor.ModuleIds;
import dev.lemonpvp.runtime.security.PathGuard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads, validates and (on first start) creates {@code config/lemon-runtime.yml}. */
public final class RuntimeConfigLoader {

    /** Current configuration format version. */
    public static final int CONFIG_VERSION = 1;

    private static final Set<String> KNOWN_KEYS = Set.of(
            "config-version", "enabled", "module-directory", "allow-runtime-deploy", "startup-modules",
            "persist-runtime-state", "verify-integrity", "trusted-hashes-file", "log-module-lifecycle",
            "max-module-count", "operation-timeout-seconds", "allowed-api-versions", "allow-server-internals", "live-patching",
            "plugin-updates");

    private static final Set<String> PLUGIN_UPDATE_KEYS = Set.of("enabled", "trusted-hashes-file");

    private static final Set<String> LIVE_PATCH_KEYS = Set.of(
            "enabled", "profile", "patch-directory", "trusted-hashes-file", "confirm-timeout-seconds");

    /** Default file content. Kept in sync with {@link RuntimeConfig#defaults()} by a unit test. */
    public static final String DEFAULT_FILE = """
            # Lemon hot-deployment runtime configuration.
            # Documentation: docs/HOT-DEPLOYMENT.md and docs/SECURITY.md in the Lemon repository.
            #
            # IMPORTANT: Modules are executable Java code with the full rights of the server
            # process. Only enable this feature if you control who can place files in the
            # module directory and who can run /fork module commands.
            config-version: 1

            # Master switch. When false (default) the runtime does not start: no threads, no
            # file scans, no commands, no listeners. The server behaves like upstream Paper.
            enabled: false

            # Directory (relative to the server directory) that contains module jars.
            # Dropping a jar here never activates it; activation is always explicit.
            module-directory: lemon/modules

            # Allow /fork module load|enable|disable|reload|unload while the server runs.
            # When false, only 'startup-modules' are activated during server start.
            allow-runtime-deploy: false

            # Module ids that are loaded and enabled when the server starts.
            startup-modules: []

            # Re-activate modules that were enabled at runtime after a restart. A module is
            # only re-activated if its jar still has the same SHA-256 digest.
            persist-runtime-state: true

            # Require a '<jar>.sha256' file (sha256sum format) next to every module jar and
            # reject jars whose digest does not match. Detects corrupt or partial uploads.
            verify-integrity: true

            # Optional allow-list (relative to the server directory) with one trusted SHA-256
            # digest per line. When set, only jars whose digest is listed can be loaded.
            trusted-hashes-file: ""

            # Log every lifecycle transition at INFO level (the audit log is always written).
            log-module-lifecycle: true

            # Maximum number of simultaneously loaded modules.
            max-module-count: 32

            # Timeout for asynchronous phases (file checks, hashing). Main-thread callbacks
            # that take longer are reported as slow; they cannot be interrupted safely.
            operation-timeout-seconds: 30

            # Module API versions accepted by this server.
            allowed-api-versions: [1]

            # Let module class loaders resolve net.minecraft.* and org.bukkit.craftbukkit.*
            # classes. Such modules break on every Minecraft update. This is NOT a security
            # boundary: reflection can always reach server internals.
            allow-server-internals: false

            # Live updates of normal Paper/Bukkit plugins. Put the new jar into plugins/update/,
            # then '/fork live check <plugin>' and '/fork live update <plugin>'. Plugins that
            # cannot be swapped safely (dependents, Paper plugins, lifecycle API, ...) are
            # reported as "restart required"; Paper applies plugins/update/ on the next start.
            plugin-updates:
              # Allow /fork live update|rollback for plugins. Checking is always possible.
              enabled: false
              # Optional allow-list (one SHA-256 per line) of plugin jars that may be installed.
              trusted-hashes-file: ""

            # Core live patching: redefine classes of the RUNNING server (Paper, Minecraft,
            # Lemon) with the JVM's class redefinition. This is a separate feature from module
            # hot deployment. It needs the JVM option -javaagent:lemon-livepatch-agent.jar and
            # 'enabled: true' above. Standard JVMs only accept changed method bodies.
            # Documentation: docs/LIVE-UPDATES.md
            live-patching:
              # Allow '/fork live update|rollback <patch>'. Status and capability probes work regardless.
              enabled: false
              # production:  patches must be listed in trusted-hashes-file (explicit approval).
              # development: allow-list optional, more diagnostics. Only honoured if the JVM was
              #              started with -Dlemon.livepatch.development=true; otherwise production.
              profile: production
              # Directory with uploaded patches: <id>.lemonpatch + <id>.lemonpatch.sha256
              patch-directory: lemon/patches/inbox
              # Allow-list (one SHA-256 per line) of approved patch archives. Required in production.
              trusted-hashes-file: ""
              # How long '/fork live update <patch>' waits for '/fork live update <patch> confirm'.
              # (Plugin and module updates always wait 120 s.)
              confirm-timeout-seconds: 120
            """;

    private RuntimeConfigLoader() {
    }

    /**
     * Result of loading the configuration.
     *
     * @param config         the parsed configuration (defaults if the file was invalid)
     * @param errors         validation errors; a non-empty list means the file must not be used
     * @param warnings       non-fatal findings, e.g. unknown keys
     * @param createdDefault whether the file did not exist and the default file was written
     */
    public record Result(RuntimeConfig config, List<String> errors, List<String> warnings, boolean createdDefault) {
        public Result {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }

        /** @return whether the configuration is valid */
        public boolean valid() {
            return this.errors.isEmpty();
        }
    }

    /**
     * Loads the configuration, writing the default file if it does not exist yet.
     *
     * @param file configuration file
     * @return the result
     */
    public static Result loadOrCreate(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, DEFAULT_FILE, StandardCharsets.UTF_8);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                return new Result(RuntimeConfig.defaults(), List.of(),
                        List.of("could not write default configuration: " + e.getMessage()), false);
            }
            return new Result(RuntimeConfig.defaults(), List.of(), List.of(), true);
        }
        return load(file);
    }

    /**
     * @param file existing configuration file
     * @return the result; on errors {@link Result#config()} contains the defaults
     */
    public static Result load(Path file) {
        Map<String, Object> raw;
        try {
            raw = YamlSupport.load(file);
        } catch (IOException | YamlSupport.YamlFormatException e) {
            return new Result(RuntimeConfig.defaults(), List.of("cannot read " + file.getFileName() + ": " + e.getMessage()),
                    List.of(), false);
        }
        return parse(raw);
    }

    /**
     * @param raw parsed YAML
     * @return the result
     */
    public static Result parse(Map<String, Object> raw) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        RuntimeConfig d = RuntimeConfig.defaults();
        for (String key : raw.keySet()) {
            if (!KNOWN_KEYS.contains(key)) {
                warnings.add("unknown key '" + PathGuard.sanitize(key) + "' is ignored");
            }
        }
        Object cv = raw.get("config-version");
        if (cv != null && !(cv instanceof Integer i && i == CONFIG_VERSION)) {
            errors.add("unsupported config-version " + PathGuard.sanitize(String.valueOf(cv)) + " (expected " + CONFIG_VERSION + ")");
        }

        boolean enabled = bool(raw, "enabled", d.enabled(), errors);
        String moduleDirectory = str(raw, "module-directory", d.moduleDirectory(), errors);
        if (!PathGuard.isSafeRelativePath(moduleDirectory)) {
            errors.add("module-directory must be a safe relative path inside the server directory");
        }
        boolean allowRuntimeDeploy = bool(raw, "allow-runtime-deploy", d.allowRuntimeDeploy(), errors);
        List<String> startup = strList(raw, "startup-modules", errors);
        for (String id : startup) {
            String p = ModuleIds.problem(id);
            if (p != null) {
                errors.add("startup-modules: " + p);
            }
        }
        if (new LinkedHashSet<>(startup).size() != startup.size()) {
            errors.add("startup-modules contains duplicates");
        }
        boolean persist = bool(raw, "persist-runtime-state", d.persistRuntimeState(), errors);
        boolean verify = bool(raw, "verify-integrity", d.verifyIntegrity(), errors);
        String trusted = str(raw, "trusted-hashes-file", d.trustedHashesFile(), errors);
        if (!trusted.isEmpty() && !PathGuard.isSafeRelativePath(trusted)) {
            errors.add("trusted-hashes-file must be empty or a safe relative path");
        }
        boolean logLifecycle = bool(raw, "log-module-lifecycle", d.logModuleLifecycle(), errors);
        int max = integer(raw, "max-module-count", d.maxModuleCount(), errors);
        if (max < 1 || max > 1024) {
            errors.add("max-module-count must be between 1 and 1024");
        }
        int timeout = integer(raw, "operation-timeout-seconds", (int) d.operationTimeout().toSeconds(), errors);
        if (timeout < 1 || timeout > 3600) {
            errors.add("operation-timeout-seconds must be between 1 and 3600");
        }
        Set<Integer> apiVersions = new LinkedHashSet<>();
        Object av = raw.get("allowed-api-versions");
        if (av == null) {
            apiVersions.addAll(d.allowedApiVersions());
        } else if (av instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Integer i && i >= 1) {
                    apiVersions.add(i);
                } else {
                    errors.add("allowed-api-versions must only contain positive integers");
                }
            }
            if (apiVersions.isEmpty()) {
                errors.add("allowed-api-versions must not be empty");
            }
        } else {
            errors.add("allowed-api-versions must be a list");
        }
        boolean internals = bool(raw, "allow-server-internals", d.allowServerInternals(), errors);
        LivePatchConfig livePatching = livePatching(raw.get("live-patching"), d.livePatching(), errors, warnings);
        PluginUpdateConfig pluginUpdates = pluginUpdates(raw.get("plugin-updates"), d.pluginUpdates(), errors, warnings);

        if (!errors.isEmpty()) {
            return new Result(d, errors, warnings, false);
        }
        return new Result(new RuntimeConfig(enabled, moduleDirectory, allowRuntimeDeploy, startup, persist, verify,
                trusted, logLifecycle, max, Duration.ofSeconds(timeout), apiVersions, internals, livePatching, pluginUpdates),
                errors, warnings, false);
    }

    private static PluginUpdateConfig pluginUpdates(Object value, PluginUpdateConfig d, List<String> errors, List<String> warnings) {
        if (value == null) {
            return d;
        }
        if (!(value instanceof Map<?, ?> map)) {
            errors.add("plugin-updates must be a section (map)");
            return d;
        }
        Map<String, Object> raw = new java.util.LinkedHashMap<>();
        map.forEach((k, v) -> raw.put(String.valueOf(k), v));
        for (String key : raw.keySet()) {
            if (!PLUGIN_UPDATE_KEYS.contains(key)) {
                warnings.add("unknown key 'plugin-updates." + PathGuard.sanitize(key) + "' is ignored");
            }
        }
        List<String> sectionErrors = new ArrayList<>();
        boolean enabled = bool(raw, "enabled", d.enabled(), sectionErrors);
        String trusted = str(raw, "trusted-hashes-file", d.trustedHashesFile(), sectionErrors);
        if (!trusted.isEmpty() && !PathGuard.isSafeRelativePath(trusted)) {
            sectionErrors.add("trusted-hashes-file must be empty or a safe relative path");
        }
        sectionErrors.forEach(e -> errors.add("plugin-updates." + e));
        return new PluginUpdateConfig(enabled, trusted);
    }

    private static LivePatchConfig livePatching(Object value, LivePatchConfig d, List<String> errors, List<String> warnings) {
        if (value == null) {
            return d;
        }
        if (!(value instanceof Map<?, ?> map)) {
            errors.add("live-patching must be a section (map)");
            return d;
        }
        Map<String, Object> raw = new java.util.LinkedHashMap<>();
        map.forEach((k, v) -> raw.put(String.valueOf(k), v));
        for (String key : raw.keySet()) {
            if (!LIVE_PATCH_KEYS.contains(key)) {
                warnings.add("unknown key 'live-patching." + PathGuard.sanitize(key) + "' is ignored");
            }
        }
        List<String> sectionErrors = new ArrayList<>();
        boolean enabled = bool(raw, "enabled", d.enabled(), sectionErrors);
        String profileText = str(raw, "profile", d.profile().yamlName(), sectionErrors);
        LivePatchConfig.Profile profile = d.profile();
        switch (profileText) {
            case "production" -> profile = LivePatchConfig.Profile.PRODUCTION;
            case "development" -> profile = LivePatchConfig.Profile.DEVELOPMENT;
            default -> sectionErrors.add("profile must be 'production' or 'development'");
        }
        String dir = str(raw, "patch-directory", d.patchDirectory(), sectionErrors);
        if (!PathGuard.isSafeRelativePath(dir)) {
            sectionErrors.add("patch-directory must be a safe relative path inside the server directory");
        }
        String trusted = str(raw, "trusted-hashes-file", d.trustedHashesFile(), sectionErrors);
        if (!trusted.isEmpty() && !PathGuard.isSafeRelativePath(trusted)) {
            sectionErrors.add("trusted-hashes-file must be empty or a safe relative path");
        }
        int confirm = integer(raw, "confirm-timeout-seconds", d.confirmTimeoutSeconds(), sectionErrors);
        if (confirm < 10 || confirm > 3600) {
            sectionErrors.add("confirm-timeout-seconds must be between 10 and 3600");
        }
        sectionErrors.forEach(e -> errors.add("live-patching." + e));
        if (enabled && profile == LivePatchConfig.Profile.PRODUCTION && trusted.isEmpty()) {
            warnings.add("live-patching: the production profile applies only patches listed in trusted-hashes-file,"
                    + " which is not set - every apply will be refused");
        }
        return new LivePatchConfig(enabled, profile, dir, trusted, confirm);
    }

    private static boolean bool(Map<String, Object> raw, String key, boolean def, List<String> errors) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        errors.add(key + " must be true or false");
        return def;
    }

    private static int integer(Map<String, Object> raw, String key, int def, List<String> errors) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Integer i) {
            return i;
        }
        errors.add(key + " must be an integer");
        return def;
    }

    private static String str(Map<String, Object> raw, String key, String def, List<String> errors) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof String s) {
            return s.trim();
        }
        errors.add(key + " must be a string");
        return def;
    }

    private static List<String> strList(Map<String, Object> raw, String key, List<String> errors) {
        Object v = raw.get(key);
        if (v == null) {
            return List.of();
        }
        if (!(v instanceof List<?> list)) {
            errors.add(key + " must be a list");
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String s) {
                out.add(s.trim());
            } else {
                errors.add(key + " must only contain strings");
            }
        }
        return out;
    }

    /**
     * Describes which changed settings cannot be applied without a restart.
     *
     * @param current running configuration
     * @param next    new configuration
     * @return names of settings that only take effect after a restart
     */
    public static List<String> restartRequiredChanges(RuntimeConfig current, RuntimeConfig next) {
        List<String> out = new ArrayList<>();
        if (current.enabled() != next.enabled()) {
            out.add("enabled");
        }
        if (!current.moduleDirectory().equals(next.moduleDirectory())) {
            out.add("module-directory");
        }
        if (!current.startupModules().equals(next.startupModules())) {
            out.add("startup-modules");
        }
        if (current.livePatching().enabled() != next.livePatching().enabled()) {
            out.add("live-patching.enabled");
        }
        if (current.livePatching().profile() != next.livePatching().profile()) {
            out.add("live-patching.profile");
        }
        if (!current.livePatching().patchDirectory().equals(next.livePatching().patchDirectory())) {
            out.add("live-patching.patch-directory");
        }
        return out;
    }
}
