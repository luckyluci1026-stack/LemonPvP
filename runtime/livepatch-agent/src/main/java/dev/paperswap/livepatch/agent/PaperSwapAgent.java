package dev.paperswap.livepatch.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that only keeps the {@link Instrumentation} instance of the JVM so that the
 * PaperSwap runtime can redefine or retransform classes when an administrator applies a live
 * patch.
 *
 * <p>Two ways to load it, both standard {@code java.lang.instrument} mechanisms:</p>
 * <ul>
 *     <li>at JVM start: {@code java -javaagent:paperswap-agent.jar -jar server.jar}
 *     ({@link #premain}); recommended;</li>
 *     <li>into a running JVM, from a second process of the same OS user:
 *     {@code java -jar paperswap-agent.jar attach <pid>} ({@link AttachLauncher},
 *     which calls {@code VirtualMachine.loadAgent}; the JVM then calls {@link #agentmain}).
 *     JDK 21 and newer print a warning for dynamically loaded agents unless the server was
 *     started with {@code -XX:+EnableDynamicAgentLoading} (JEP 451).</li>
 * </ul>
 *
 * <p>The agent registers no class file transformer, changes no class and starts no thread;
 * without an explicit, confirmed live-patch command nothing is redefined. The jar declares
 * {@code Can-Redefine-Classes: true} (redefinition patches) and {@code Can-Retransform-Classes:
 * true} (retransformation patches and reading back the bytecode the JVM runs after a
 * patch).</p>
 *
 * <p>Security: any code running in the server process (plugins, modules) can look this
 * class up and obtain the instance. Plugins already run with full rights, but the agent
 * widens what they can do, so only load it on servers where live patching is used.
 * Dynamic attach needs local access as the OS user that runs the server (the JDK's attach
 * mechanism, no network port).</p>
 */
public final class PaperSwapAgent {

    /**
     * Version of the agent. The static methods below are the same in all versions;
     * version 2 also declares {@code Can-Retransform-Classes}; version 3 can also be loaded
     * into a running JVM ({@code Agent-Class}, {@link #agentmain}).
     */
    public static final int VERSION = 3;

    /** {@link #loadedBy()} after {@code -javaagent}. */
    public static final String JAVAAGENT = "-javaagent";
    /** {@link #loadedBy()} after a dynamic attach. */
    public static final String AGENTMAIN = "agentmain (dynamic attach)";

    private static volatile Instrumentation instrumentation;
    private static volatile String loadedBy = "not loaded";

    private PaperSwapAgent() {
    }

    /**
     * Called by the JVM for {@code -javaagent:paperswap-agent.jar}.
     *
     * @param args            agent options (ignored)
     * @param instrumentation the JVM's instrumentation instance
     */
    public static void premain(String args, Instrumentation instrumentation) {
        install(instrumentation, JAVAAGENT);
    }

    /**
     * Called by the JVM when the agent is loaded into a running JVM
     * ({@code VirtualMachine.loadAgent}).
     *
     * @param args            agent options (ignored)
     * @param instrumentation the JVM's instrumentation instance
     */
    public static void agentmain(String args, Instrumentation instrumentation) {
        install(instrumentation, AGENTMAIN);
    }

    private static synchronized void install(Instrumentation inst, String how) {
        if (instrumentation != null) {
            System.err.println("[PaperSwap] live-patch agent " + VERSION + " is already loaded (" + loadedBy + "); " + how
                    + " ignored");
            return;
        }
        instrumentation = inst;
        loadedBy = how;
        System.err.println("[PaperSwap] live-patch agent " + VERSION + " loaded (" + how + "); class redefinition is"
                + (inst.isRedefineClassesSupported() ? " available to /fork live" : " NOT supported by this JVM")
                + (inst.isRetransformClassesSupported() ? "" : "; retransformation is NOT supported"));
    }

    /** @return the instrumentation instance, or {@code null} if the agent was not started by the JVM */
    public static Instrumentation instrumentation() {
        return instrumentation;
    }

    /** @return how the agent was loaded: {@link #JAVAAGENT}, {@link #AGENTMAIN} or {@code "not loaded"} */
    public static String loadedBy() {
        return loadedBy;
    }
}
