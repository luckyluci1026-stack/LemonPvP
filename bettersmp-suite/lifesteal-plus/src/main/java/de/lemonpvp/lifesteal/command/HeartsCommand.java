package de.lemonpvp.lifesteal.command;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /hearts [Spieler]
 */
public final class HeartsCommand implements CommandExecutor {

    private final LifestealPlus plugin;

    public HeartsCommand(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1) {
            OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(args[0]);
            if (target == null) {
                plugin.msgs().send(sender, "player-not-found");
                return true;
            }
            plugin.msgs().send(sender, "hearts-other",
                    "player", String.valueOf(target.getName()),
                    "hearts", String.valueOf(plugin.hearts().getHearts(target.getUniqueId())));
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        plugin.msgs().send(player, "hearts-self",
                "hearts", String.valueOf(plugin.hearts().getHearts(player.getUniqueId())));
        return true;
    }
}
