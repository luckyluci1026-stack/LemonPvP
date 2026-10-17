package dev.paperswap.runtime.plugins;

import java.util.ArrayList;
import java.util.List;

/**
 * What the server has registered for one plugin instance, as far as the server API can
 * list it: event listeners, Bukkit scheduler tasks, commands, services and plugin
 * messaging channels.
 *
 * <p>Not covered (no server API lists them per plugin): tasks of Paper's asynchronous and
 * region schedulers (Paper cancels them on disable), threads and executors the plugin
 * starts itself (see {@link PluginUpdateManager} for the thread check after unloading),
 * and registrations in other plugins' APIs (packet listeners, placeholder expansions,
 * permission plugin hooks ...).</p>
 *
 * @param listeners registered event listeners (handler registrations)
 * @param tasks     pending Bukkit scheduler tasks
 * @param commands  commands owned by the plugin
 * @param services  service registrations
 * @param channels  incoming and outgoing plugin messaging channels
 */
public record PluginResources(int listeners, int tasks, int commands, int services, int channels) {

    /** No resources. */
    public static final PluginResources NONE = new PluginResources(0, 0, 0, 0, 0);

    /** @return whether nothing is registered */
    public boolean isEmpty() {
        return this.listeners == 0 && this.tasks == 0 && this.commands == 0 && this.services == 0 && this.channels == 0;
    }

    /** @return e.g. {@code "3 listeners, 1 task, 2 commands"}, or {@code "nothing"} */
    public String describe() {
        List<String> parts = new ArrayList<>();
        add(parts, this.listeners, "listener");
        add(parts, this.tasks, "scheduler task");
        add(parts, this.commands, "command");
        add(parts, this.services, "service");
        add(parts, this.channels, "messaging channel");
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    private static void add(List<String> parts, int count, String noun) {
        if (count > 0) {
            parts.add(count + " " + noun + (count == 1 ? "" : "s"));
        }
    }
}
