package dev.paperswap.runtime.api.paper;

import java.util.List;
import org.bukkit.command.CommandSender;

/** Executor (and optional tab completer) of a module command. Runs on the main thread. */
@FunctionalInterface
public interface ModuleCommandHandler {

    /**
     * @param sender sender
     * @param label  label used
     * @param args   arguments
     * @return {@code false} to show the usage message
     */
    boolean execute(CommandSender sender, String label, String[] args);

    /**
     * @param sender sender
     * @param alias  alias used
     * @param args   arguments typed so far
     * @return suggestions
     */
    default List<String> complete(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
