package de.lemonpvp.lifesteal.command;

import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /revive <Spieler> - belebt einen eliminierten Spieler wieder.
 * Kostet den Ausführenden ggf. ein Herz (config revive.cost-heart).
 */
public final class ReviveCommand implements CommandExecutor {

    private final LifestealPlus plugin;

    public ReviveCommand(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lifesteal.revive")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.msgs().send(sender, "player-not-found");
            return true;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            plugin.msgs().send(sender, "player-not-found");
            return true;
        }
        UUID targetId = target.getUniqueId();
        if (!plugin.hearts().isEliminated(targetId)) {
            plugin.msgs().send(sender, "revive-not-eliminated");
            return true;
        }

        boolean costHeart = plugin.getConfig().getBoolean("revive.cost-heart", true);
        if (costHeart && sender instanceof Player reviver
                && !reviver.hasPermission("lifesteal.admin")) {
            int have = plugin.hearts().getHearts(reviver.getUniqueId());
            if (have < 2) {
                plugin.msgs().send(reviver, "revive-need-heart");
                return true;
            }
            if (plugin.eliminations().revive(targetId, reviver)) {
                plugin.hearts().addHearts(reviver.getUniqueId(), -1);
            }
            return true;
        }

        plugin.eliminations().revive(targetId, sender);
        return true;
    }
}
