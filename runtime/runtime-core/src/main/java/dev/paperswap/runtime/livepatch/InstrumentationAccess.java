package dev.paperswap.runtime.livepatch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Locates the {@link Instrumentation} instance kept by {@code paperswap-agent.jar}.
 *
 * <p>The agent is loaded by the JVM's system class loader ({@code -javaagent}, and also
 * after a dynamic attach, which appends the agent jar to the system class path); the server
 * (and therefore the runtime) runs in Paperclip's own class loader, which does not
 * delegate to the system class loader. The agent class is therefore looked up explicitly
 * in the system class loader and called reflectively.</p>
 */
public final class InstrumentationAccess {

    /** Fully qualified name of the agent class. */
    public static final String AGENT_CLASS = "dev.paperswap.livepatch.agent.PaperSwapAgent";
    /**
     * Agent class of {@code lemon-livepatch-agent.jar} (before the rename to PaperSwap). Its
     * static methods are the same, so a server started with the old jar still works.
     */
    public static final String LEGACY_AGENT_CLASS = "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent";

    private InstrumentationAccess() {
    }

    /**
     * Result of the lookup.
     *
     * @param instrumentation the instance, if the agent was started by the JVM
     * @param agentVersion    agent contract version, or {@code 0}
     * @param loadedBy        how the agent was loaded ({@code -javaagent}, {@code agentmain (dynamic attach)}), or
     *                        {@code "not loaded"}
     * @param detail          human readable explanation
     */
    public record Lookup(Optional<Instrumentation> instrumentation, int agentVersion, String loadedBy, String detail) {
    }

    /** The agent class in the system class loader: the current one, else the pre-rename one. */
    private static Class<?> agentClass() throws ClassNotFoundException {
        try {
            return Class.forName(AGENT_CLASS, true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            return Class.forName(LEGACY_AGENT_CLASS, true, ClassLoader.getSystemClassLoader());
        }
    }

    /** @return the lookup result; never throws */
    public static Lookup find() {
        Class<?> agent;
        try {
            agent = agentClass();
        } catch (ClassNotFoundException | LinkageError e) {
            return new Lookup(Optional.empty(), 0, "not loaded", "live-patch agent not loaded: start the JVM with"
                    + " -javaagent:paperswap-agent.jar (or attach it: java -jar paperswap-agent.jar attach <pid>)");
        }
        try {
            Method get = agent.getMethod("instrumentation");
            Object inst = get.invoke(null);
            int version = agent.getField("VERSION").getInt(null);
            String how = String.valueOf(agent.getMethod("loadedBy").invoke(null));
            if (!(inst instanceof Instrumentation instrumentation)) {
                return new Lookup(Optional.empty(), version, "not loaded",
                        "agent class is on the class path but was not started by the JVM (use -javaagent, not -cp)");
            }
            String legacy = agent.getName().equals(LEGACY_AGENT_CLASS)
                    ? " (old lemon-livepatch-agent.jar; replace it with paperswap-agent.jar)" : "";
            return new Lookup(Optional.of(instrumentation), version, how, "agent " + version + " loaded via " + how + legacy);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new Lookup(Optional.empty(), 0, "not loaded", "incompatible live-patch agent: " + e);
        }
    }
}
