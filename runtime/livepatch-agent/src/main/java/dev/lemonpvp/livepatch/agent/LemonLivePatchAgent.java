package dev.lemonpvp.livepatch.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that only keeps the {@link Instrumentation} instance of the JVM so that the
 * Lemon runtime can redefine classes when an administrator applies a live patch.
 *
 * <p>Start the server with {@code -javaagent:lemon-livepatch-agent.jar}. The agent
 * registers no class file transformer, changes no class and starts no thread; without an
 * explicit {@code /fork live update <patch> confirm} nothing is redefined. The jar declares
 * {@code Can-Redefine-Classes: true} (applying patches) and {@code Can-Retransform-Classes:
 * true} (the runtime reads back the bytecode the JVM runs after a patch, to verify it).</p>
 *
 * <p>Security: any code running in the server process (plugins, modules) can look this
 * class up and obtain the instance. Plugins already run with full rights, but the agent
 * widens what they can do, so only load it on servers where live patching is used.</p>
 */
public final class LemonLivePatchAgent {

    /**
     * Version of the agent. The static methods below are the same in all versions;
     * version 2 also declares {@code Can-Retransform-Classes} (verification after a patch).
     */
    public static final int VERSION = 2;

    private static volatile Instrumentation instrumentation;
    private static volatile String loadedBy = "not loaded";

    private LemonLivePatchAgent() {
    }

    /**
     * Called by the JVM for {@code -javaagent:lemon-livepatch-agent.jar}.
     *
     * @param args            agent options (ignored)
     * @param instrumentation the JVM's instrumentation instance
     */
    public static void premain(String args, Instrumentation instrumentation) {
        install(instrumentation, "-javaagent");
    }

    private static synchronized void install(Instrumentation inst, String how) {
        if (instrumentation != null) {
            return;
        }
        instrumentation = inst;
        loadedBy = how;
        System.err.println("[Lemon] live-patch agent " + VERSION + " loaded (" + how + "); class redefinition is"
                + (inst.isRedefineClassesSupported() ? " available to /fork live" : " NOT supported by this JVM")
                + (inst.isRetransformClassesSupported() ? "" : "; reading back running bytecode is NOT supported"));
    }

    /** @return the instrumentation instance, or {@code null} if the agent was not started by the JVM */
    public static Instrumentation instrumentation() {
        return instrumentation;
    }

    /** @return how the agent was loaded, e.g. {@code "-javaagent"} */
    public static String loadedBy() {
        return loadedBy;
    }
}
