package dev.lemonpvp.runtime.paper;

import java.util.logging.Level;
import org.bukkit.Server;

/**
 * The only entry points from Paper code into the runtime. Called from three one-line hooks
 * in {@code org.bukkit.craftbukkit.CraftServer} (see
 * {@code versions/<mc>/lemon-server/paper-patches/files/.../CraftServer.java.patch}).
 *
 * <p>Every hook catches all exceptions: a problem in the runtime must never prevent the
 * server from starting, reloading or stopping. When hot deployment is disabled the only
 * work done here is reading one small configuration file during start-up.</p>
 */
public final class LemonRuntimeHooks {

    private static PaperRuntime current;

    private LemonRuntimeHooks() {
    }

    /**
     * After all plugins were enabled (server start and {@code /reload}), before commands are
     * sent to players.
     *
     * @param server the server
     */
    public static synchronized void onPluginsEnabled(Server server) {
        try {
            if (current != null) {
                current.registerCommand(); // e.g. after /reload cleared the command map
                return;
            }
            current = PaperRuntime.start(server).orElse(null);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError vme) {
                throw vme;
            }
            current = null;
            server.getLogger().log(Level.SEVERE, "[Lemon] The hot-deployment runtime failed to start; the server continues"
                    + " without it", t);
        }
    }

    /** Before plugins are disabled during shutdown. */
    public static synchronized void onPluginsDisabling() {
        stop("shutdown");
    }

    /** Before {@code /reload} clears commands, listeners and plugins. */
    public static synchronized void onReloadStarting() {
        stop("reload");
    }

    private static void stop(String reason) {
        PaperRuntime runtime = current;
        current = null;
        if (runtime == null) {
            return;
        }
        try {
            runtime.stop();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError vme) {
                throw vme;
            }
            java.util.logging.Logger.getLogger("Lemon").log(Level.SEVERE, "[Lemon] Error while stopping the runtime (" + reason + ")", t);
        }
    }
}
