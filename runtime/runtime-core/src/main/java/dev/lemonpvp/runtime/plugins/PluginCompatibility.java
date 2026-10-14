package dev.lemonpvp.runtime.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decides whether a plugin jar can be installed or updated while the server runs. The
 * rules are deliberately conservative: anything Lemon cannot hand over safely is reported
 * as {@link Status#RESTART_REQUIRED} with the reason, never attempted live.
 */
public final class PluginCompatibility {

    private PluginCompatibility() {
    }

    /** Overall verdict. */
    public enum Status {
        /** Can be installed/updated live. */
        LIVE,
        /** Works, but only after a server restart. */
        RESTART_REQUIRED,
        /** Will not work, not even after a restart. */
        INCOMPATIBLE
    }

    /**
     * Facts about the server.
     *
     * @param minecraftVersion running Minecraft version (e.g. {@code 1.21.11} or {@code 26.2})
     * @param javaFeature      running Java feature version
     * @param unloadProblem    why plugins cannot be unloaded on this server, or {@code null}
     */
    public record Host(String minecraftVersion, int javaFeature, String unloadProblem) {
    }

    /**
     * Result.
     *
     * @param status   verdict
     * @param reasons  why the status is not {@link Status#LIVE}
     * @param warnings things to check after a live update
     */
    public record Verdict(Status status, List<String> reasons, List<String> warnings) {
        public Verdict {
            reasons = List.copyOf(reasons);
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * @param candidate inspected jar
     * @param installed currently installed plugin with the same name, if any
     * @param all       every installed plugin
     * @param host      server facts
     * @return the verdict
     */
    public static Verdict evaluate(PluginJar candidate, Optional<InstalledPlugin> installed, List<InstalledPlugin> all, Host host) {
        List<String> incompatible = new ArrayList<>(candidate.problems());
        List<String> restart = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        PluginInfo info = candidate.info();
        if (info == null) {
            if (incompatible.isEmpty()) {
                incompatible.add("the jar has no readable plugin descriptor");
            }
            return new Verdict(Status.INCOMPATIBLE, incompatible, warnings);
        }
        if (!info.apiVersion().isEmpty() && compareVersions(info.apiVersion(), host.minecraftVersion()) > 0) {
            incompatible.add("plugin needs api-version " + info.apiVersion() + " but the server runs " + host.minecraftVersion());
        }
        for (String dep : info.depend()) {
            Optional<InstalledPlugin> d = all.stream().filter(p -> p.info().answersTo(dep)).findFirst();
            if (d.isEmpty()) {
                incompatible.add("required plugin '" + dep + "' is not installed");
            } else if (!d.get().enabled()) {
                incompatible.add("required plugin '" + dep + "' is installed but not enabled");
            }
        }
        for (String dep : info.softDepend()) {
            if (all.stream().noneMatch(p -> p.info().answersTo(dep))) {
                warnings.add("optional plugin '" + dep + "' is not installed");
            }
        }

        if (info.kind() == PluginInfo.Kind.PAPER) {
            restart.add("it is a Paper plugin (paper-plugin.yml): its bootstrap and loader phases only run at server start");
        }
        if (!candidate.lifecycleApiClasses().isEmpty()) {
            restart.add("it uses Paper's lifecycle API (e.g. Brigadier command or registry registration) in "
                    + candidate.lifecycleApiClasses() + "; such handlers only run at server start");
        }
        if (installed.isPresent()) {
            InstalledPlugin old = installed.get();
            if (old.info().kind() == PluginInfo.Kind.PAPER) {
                restart.add("the installed version is a Paper plugin; Paper plugins cannot be unloaded safely");
            }
            List<String> dependents = new ArrayList<>();
            for (InstalledPlugin p : all) {
                if (p == old || !p.enabled()) {
                    continue;
                }
                boolean uses = p.info().depend().stream().anyMatch(old.info()::answersTo)
                        || p.info().softDepend().stream().anyMatch(old.info()::answersTo)
                        || old.info().loadBefore().stream().anyMatch(p.info()::answersTo);
                if (uses) {
                    dependents.add(p.name());
                }
            }
            if (!dependents.isEmpty()) {
                restart.add("enabled plugins depend on " + old.name() + " and may hold references to its classes: " + dependents);
            }
            if (host.unloadProblem() != null) {
                restart.add("this server cannot unload plugins: " + host.unloadProblem());
            }
            int cmp = compareVersions(info.version(), old.info().version());
            if (cmp == 0) {
                warnings.add("same version (" + info.version() + ") as the installed plugin");
            } else if (cmp < 0) {
                warnings.add("this is a downgrade from " + old.info().version() + " to " + info.version());
            }
            if (!old.info().main().equals(info.main())) {
                warnings.add("main class changed from " + old.info().main() + " to " + info.main());
            }
        }
        if (info.loadAtStartup()) {
            warnings.add("load: STARTUP - the plugin normally starts before worlds are loaded; live it starts afterwards");
        }
        if (info.hasLibraries()) {
            warnings.add("declares libraries: the server resolves (and may download) them while loading");
        }
        if (!candidate.internalsClasses().isEmpty()) {
            warnings.add("uses server internals (net.minecraft / CraftBukkit) in " + candidate.internalsClasses()
                    + "; it only works on the server version it was built for");
        }
        warnings.add("objects, threads or static state the old version created outside the Bukkit API are not cleaned up by Paper");

        Status status = !incompatible.isEmpty() ? Status.INCOMPATIBLE : !restart.isEmpty() ? Status.RESTART_REQUIRED : Status.LIVE;
        List<String> reasons = new ArrayList<>(incompatible);
        reasons.addAll(restart);
        return new Verdict(status, reasons, warnings);
    }

    /**
     * Compares dotted versions numerically where possible ({@code 1.21} < {@code 1.21.11} < {@code 26.1}).
     * Non-numeric parts compare as text.
     *
     * @param a version
     * @param b version
     * @return negative, zero or positive
     */
    public static int compareVersions(String a, String b) {
        String[] pa = a.split("[.\\-+]");
        String[] pb = b.split("[.\\-+]");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            String x = i < pa.length ? pa[i] : "0";
            String y = i < pb.length ? pb[i] : "0";
            int c;
            if (x.matches("\\d{1,9}") && y.matches("\\d{1,9}")) {
                c = Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
            } else {
                c = x.compareTo(y);
            }
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
