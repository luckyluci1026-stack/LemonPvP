package dev.lemonpvp.runtime.livepatch;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What this JVM offers for instrumentation and what is missing, with the fix for each gap.
 * Facts come from the JVM ({@link Instrumentation}, VM options, JVM arguments), never from
 * vendor names. Used at server start and by {@code /fork instrumentation status}.
 */
public final class InstrumentationDiagnostics {

    private InstrumentationDiagnostics() {
    }

    /** How a finding is shown. */
    public enum Level {
        /** Available. */
        OK,
        /** Fact or limitation that needs no action. */
        INFO,
        /** Something a live-patch feature needs is missing; the text says how to fix it. */
        MISSING,
        /** A configuration that is risky. */
        WARNING
    }

    /**
     * @param level level
     * @param text  one line
     */
    public record Finding(Level level, String text) {
    }

    /**
     * Loaded classes, counted once.
     *
     * @param loaded     loaded classes without arrays and primitives
     * @param hidden     hidden classes among them (never modifiable)
     * @param modifiable classes for which {@link Instrumentation#isModifiableClass} is true
     * @param micros     time the count took
     */
    public record ClassCounts(int loaded, int hidden, int modifiable, long micros) {
    }

    /**
     * @param findings    findings, most important first
     * @param classCounts counts, if the agent is loaded and counting was requested
     */
    public record Report(List<Finding> findings, Optional<ClassCounts> classCounts) {
        public Report {
            findings = List.copyOf(findings);
        }

        /** @return whether something is {@link Level#MISSING} or {@link Level#WARNING} */
        public boolean hasProblems() {
            return this.findings.stream().anyMatch(f -> f.level() == Level.MISSING || f.level() == Level.WARNING);
        }
    }

    /**
     * @param lookup              agent lookup
     * @param livePatchingEnabled {@code live-patching.enabled}
     * @param countClasses        also count loaded and modifiable classes (one pass over all loaded classes)
     * @return the report
     */
    public static Report collect(InstrumentationAccess.Lookup lookup, boolean livePatchingEnabled, boolean countClasses) {
        List<Finding> out = new ArrayList<>();
        int feature = Runtime.version().feature();
        out.add(new Finding(Level.INFO, "JVM: " + JvmCapabilities.vmDescription() + " (Java " + feature + ")"));
        Optional<Instrumentation> inst = lookup.instrumentation();
        String disableAttach = JvmCapabilities.vmOption("DisableAttachMechanism");
        if (inst.isEmpty()) {
            out.add(new Finding(Level.MISSING, lookup.detail()));
            out.add(new Finding(Level.INFO, "fix: add -javaagent:lemon-livepatch-agent.jar before -jar in the start command"
                    + " (recommended), or attach it to this running server: java -jar lemon-livepatch-agent.jar attach "
                    + ProcessHandle.current().pid() + (disableAttach.equals("true")
                    ? " - NOT possible here: the JVM runs with -XX:+DisableAttachMechanism" : " (same OS user, needs a JDK)")));
        } else {
            Instrumentation i = inst.get();
            out.add(new Finding(Level.OK, "live-patch agent " + lookup.agentVersion() + " loaded via " + lookup.loadedBy()));
            if (lookup.agentVersion() < 2) {
                out.add(new Finding(Level.MISSING, "agent " + lookup.agentVersion() + " does not declare Can-Retransform-Classes:"
                        + " retransformation patches and the read-back check need the agent jar of this build"));
            } else if (lookup.agentVersion() < 3) {
                out.add(new Finding(Level.INFO, "agent " + lookup.agentVersion() + " is older than this build's agent 3"
                        + " (no dynamic attach); everything else works"));
            }
            out.add(new Finding(i.isRedefineClassesSupported() ? Level.OK : Level.MISSING, "redefineClasses: "
                    + (i.isRedefineClassesSupported() ? "supported" : "NOT supported by this JVM (no redefinition patches)")));
            out.add(new Finding(i.isRetransformClassesSupported() ? Level.OK : Level.MISSING, "retransformClasses: "
                    + (i.isRetransformClassesSupported() ? "supported" : "NOT supported (agent manifest or JVM): no retransformation"
                    + " patches, no read-back check")));
            if (lookup.loadedBy().startsWith("agentmain")) {
                out.add(new Finding(Level.INFO, "the agent was attached to the running JVM. Classes loaded before the attach are"
                        + " retransformed from bytes the JVM reconstitutes (equivalent, not byte-identical); Java 21+ printed a"
                        + " warning (JEP 451). For production prefer -javaagent at start"));
            }
            if (countClasses) {
                ClassCounts counts = count(i);
                out.add(new Finding(Level.INFO, "classes: " + counts.loaded() + " loaded, " + counts.modifiable() + " modifiable, "
                        + counts.hidden() + " hidden (never modifiable); arrays and primitives are never modifiable (counted in "
                        + counts.micros() / 1000 + " ms)"));
                return finish(out, livePatchingEnabled, disableAttach, Optional.of(counts));
            }
        }
        return finish(out, livePatchingEnabled, disableAttach, Optional.empty());
    }

    private static Report finish(List<Finding> out, boolean livePatchingEnabled, String disableAttach, Optional<ClassCounts> counts) {
        String enhanced = JvmCapabilities.enhancedRedefinitionFlag();
        if (enhanced.equals("true")) {
            out.add(new Finding(Level.INFO, "AllowEnhancedClassRedefinition: true - structural changes may be possible (only"
                    + " the probes decide: /fork live patch probe). Use such a JVM for development servers only"));
        } else {
            out.add(new Finding(Level.INFO, "standard class redefinition: only method bodies, constants and attributes can change"
                    + " live; adding/removing methods or fields and hierarchy changes need a restart (or, on development servers,"
                    + " JetBrains Runtime with -XX:+AllowEnhancedClassRedefinition)"));
        }
        if (Runtime.version().feature() >= 21) {
            out.add(new Finding(Level.INFO, "dynamic attach: EnableDynamicAgentLoading=" + JvmCapabilities.vmOption("EnableDynamicAgentLoading")
                    + ", DisableAttachMechanism=" + disableAttach));
        }
        jdwp().ifPresent(out::add);
        if (!livePatchingEnabled) {
            out.add(new Finding(Level.INFO, "live-patching.enabled is false: inspection works, patch commands are refused"
                    + " (set it to true and restart to apply patches)"));
        }
        return new Report(out, counts);
    }

    /**
     * Counts loaded and modifiable classes.
     *
     * @param inst instrumentation
     * @return counts
     */
    public static ClassCounts count(Instrumentation inst) {
        long start = System.nanoTime();
        int loaded = 0;
        int hidden = 0;
        int modifiable = 0;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.isArray() || c.isPrimitive()) {
                continue;
            }
            loaded++;
            if (c.isHidden()) {
                hidden++;
            }
            if (inst.isModifiableClass(c)) {
                modifiable++;
            }
        }
        return new ClassCounts(loaded, hidden, modifiable, (System.nanoTime() - start) / 1000);
    }

    /** @return a finding about a JDWP debug agent in the JVM arguments, if there is one */
    static Optional<Finding> jdwp() {
        List<String> args;
        try {
            args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (RuntimeException | LinkageError e) {
            return Optional.empty();
        }
        return jdwp(args);
    }

    /**
     * @param jvmArgs JVM arguments
     * @return a finding about a JDWP debug agent, if one is configured
     */
    static Optional<Finding> jdwp(List<String> jvmArgs) {
        for (String arg : jvmArgs) {
            if (!(arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp"))) {
                continue;
            }
            String address = null;
            boolean server = false;
            for (String part : arg.substring(arg.indexOf(arg.startsWith("-Xrunjdwp") ? ':' : '=') + 1).split(",")) {
                if (part.startsWith("address=")) {
                    address = part.substring("address=".length());
                } else if (part.equals("server=y")) {
                    server = true;
                }
            }
            if (!server) {
                return Optional.of(new Finding(Level.INFO, "JDWP debug agent connects out to " + address + " (server=n)"));
            }
            String host = address == null || !address.contains(":") ? "localhost" : address.substring(0, address.lastIndexOf(':'));
            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("[::1]") || h.equals("::1")) {
                return Optional.of(new Finding(Level.INFO, "JDWP debug port listens on " + (address == null ? "an ephemeral port" : address)
                        + " on the loopback interface only (reach it through an SSH tunnel)"));
            }
            return Optional.of(new Finding(Level.WARNING, "JDWP debug port listens on " + address + ": anyone who can reach it can"
                    + " run any code in this server. Bind it to 127.0.0.1 (address=127.0.0.1:5005) and use an SSH tunnel, or remove it"));
        }
        return Optional.empty();
    }
}
