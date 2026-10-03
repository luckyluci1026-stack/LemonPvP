package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /spawn - zum gesetzten Serverspawn. */
public final class SpawnCommand implements CommandExecutor {

    private final BetterSMP plugin;

    public SpawnCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!spieler.hasPermission("bettersmp.spawn")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (!plugin.spawn().bringe(spieler)) {
            plugin.msgs().send(spieler, "spawn.not-set");
            return true;
        }
        plugin.msgs().send(spieler, "spawn.teleported");
        return true;
    }
}
