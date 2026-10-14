package dev.lemonpvp.runtime.livepatch;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Locates the {@link Instrumentation} instance kept by {@code lemon-livepatch-agent.jar}.
 *
 * <p>The agent is loaded by the JVM's system class loader ({@code -javaagent}); the server
 * (and therefore the runtime) runs in Paperclip's own class loader, which does not
 * delegate to the system class loader. The agent class is therefore looked up explicitly
 * in the system class loader and called reflectively.</p>
 */
public final class InstrumentationAccess {

    /** Fully qualified name of the agent class. */
    public static final String AGENT_CLASS = "dev.lemonpvp.livepatch.agent.LemonLivePatchAgent";

    private InstrumentationAccess() {
    }

    /**
     * Result of the lookup.
     *
     * @param instrumentation the instance, if the agent was started by the JVM
     * @param agentVersion    agent contract version, or {@code 0}
     * @param detail          human readable explanation
     */
    public record Lookup(Optional<Instrumentation> instrumentation, int agentVersion, String detail) {
    }

    /** @return the lookup result; never throws */
    public static Lookup find() {
        Class<?> agent;
        try {
            agent = Class.forName(AGENT_CLASS, true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return new Lookup(Optional.empty(), 0,
                    "live-patch agent not loaded: start the JVM with -javaagent:lemon-livepatch-agent.jar");
        }
        try {
            Method get = agent.getMethod("instrumentation");
            Object inst = get.invoke(null);
            int version = agent.getField("VERSION").getInt(null);
            String how = String.valueOf(agent.getMethod("loadedBy").invoke(null));
            if (!(inst instanceof Instrumentation instrumentation)) {
                return new Lookup(Optional.empty(), version,
                        "agent class is on the class path but was not started by the JVM (use -javaagent, not -cp)");
            }
            return new Lookup(Optional.of(instrumentation), version, "agent " + version + " loaded via " + how);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new Lookup(Optional.empty(), 0, "incompatible live-patch agent: " + e);
        }
    }
}
