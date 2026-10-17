package dev.lemonpvp.runtime.livepatch;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Facts about the running JVM's class redefinition support. Probe results are cached for
 * the lifetime of the JVM (they cannot change while it runs).
 */
public final class JvmCapabilities {

    private static final Map<Capability, CapabilityProbe.Result> PROBES = new EnumMap<>(Capability.class);
    private static Instant lastProbe;

    private JvmCapabilities() {
    }

    /** @return e.g. {@code OpenJDK 64-Bit Server VM (Eclipse Adoptium) 21.0.5+11-LTS} */
    public static String vmDescription() {
        return System.getProperty("java.vm.name", "?") + " (" + System.getProperty("java.vm.vendor", "?") + ") "
                + System.getProperty("java.runtime.version", System.getProperty("java.version", "?"));
    }

    /**
     * Value of the JetBrains Runtime / DCEVM flag {@code AllowEnhancedClassRedefinition}.
     *
     * @return {@code "true"}, {@code "false"} or a note that the JVM does not know the flag
     */
    public static String enhancedRedefinitionFlag() {
        return vmOption("AllowEnhancedClassRedefinition");
    }

    /**
     * Why Lemon does not retransform with changes on a JVM with enhanced redefinition.
     * Measured in CI with {@code scripts/ci/RetransformRepro.java}: JetBrains Runtime 21.0.11
     * and 25.0.4.1 with {@code -XX:+AllowEnhancedClassRedefinition} aborted ("free(): double free
     * detected") when a class that a transformer had changed during a retransformation was
     * unloaded; the same JVMs without the flag, and every scenario without a changing
     * transformer, ran through.
     */
    public static final String ENHANCED_RETRANSFORM_PROBLEM = "this JVM runs with AllowEnhancedClassRedefinition=true;"
            + " there, unloading a class that a transformer had changed during a retransformation crashed the JVM"
            + " (\"double free\", JetBrains Runtime 21 and 25 in Lemon's CI), so Lemon does not apply retransformation"
            + " patches on such a JVM - use 'redefine'";

    /** @return whether the JVM runs with {@code AllowEnhancedClassRedefinition=true} */
    public static boolean enhancedRedefinition() {
        return "true".equals(enhancedRedefinitionFlag());
    }

    /**
     * Value of a HotSpot VM option (e.g. {@code EnableDynamicAgentLoading}).
     *
     * @param name option name without {@code -XX:}
     * @return {@code "true"}, {@code "false"}, another value, or a note that the JVM does not know the option
     */
    public static String vmOption(String name) {
        try {
            var bean = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
            if (bean == null) {
                return "unknown (no HotSpot diagnostic bean)";
            }
            return bean.getVMOption(name).getValue();
        } catch (IllegalArgumentException e) {
            return "not available in this JVM";
        } catch (RuntimeException | LinkageError e) {
            return "unknown (" + e.getClass().getSimpleName() + ")";
        }
    }

    /** @return cached probe results (may be incomplete) */
    public static synchronized Map<Capability, CapabilityProbe.Result> probed() {
        return Collections.unmodifiableMap(new EnumMap<>(PROBES));
    }

    /** @return time of the last probe run, or {@code null} */
    public static synchronized Instant lastProbe() {
        return lastProbe;
    }

    /**
     * Probes the capabilities that were not probed yet.
     *
     * @param instrumentation instrumentation of this JVM
     * @param needed          capabilities of interest
     * @return results for {@code needed}
     */
    public static synchronized Map<Capability, CapabilityProbe.Result> ensure(Instrumentation instrumentation, Set<Capability> needed) {
        Map<Capability, CapabilityProbe.Result> out = new EnumMap<>(Capability.class);
        for (Capability c : needed) {
            CapabilityProbe.Result r = PROBES.get(c);
            if (r == null) {
                r = CapabilityProbe.probe(instrumentation, c);
                PROBES.put(c, r);
                lastProbe = Instant.now();
            }
            out.put(c, r);
        }
        return out;
    }

    /**
     * Probes every capability again.
     *
     * @param instrumentation instrumentation of this JVM
     * @return all results
     */
    public static synchronized Map<Capability, CapabilityProbe.Result> probeAll(Instrumentation instrumentation) {
        PROBES.putAll(CapabilityProbe.probeAll(instrumentation));
        lastProbe = Instant.now();
        return probed();
    }
}
