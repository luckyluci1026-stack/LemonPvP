package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /setspawn - setzt den Serverspawn auf die eigene Position. */
public final class SetSpawnCommand implements CommandExecutor {

    private final BetterSMP plugin;

    public SetSpawnCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!spieler.hasPermission("bettersmp.setspawn")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        plugin.spawn().setze(spieler.getLocation());
        plugin.msgs().send(spieler, "spawn.set");
        return true;
    }
}
