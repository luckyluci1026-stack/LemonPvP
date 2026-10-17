package dev.paperswap.runtime.commands;

/** Whoever executes an administrative command (console or player), as seen by the runtime. */
public interface CommandActor {

    /** @return name used in the audit log */
    String name();

    /**
     * @param node permission node
     * @return whether the actor has the permission
     */
    boolean hasPermission(String node);

    /**
     * Sends one line of output. {@code text} is plain text; implementations must not
     * interpret formatting codes or markup contained in it.
     *
     * @param level message level
     * @param text  plain text
     */
    void send(MessageLevel level, String text);
}
