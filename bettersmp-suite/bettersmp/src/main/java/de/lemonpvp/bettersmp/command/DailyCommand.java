package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /daily - eine taegliche Kleinigkeit, mit Bonus fuer die Serie. */
public final class DailyCommand implements CommandExecutor {

    private final BetterSMP plugin;

    public DailyCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!spieler.hasPermission("bettersmp.daily")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (plugin.dailyReward().bereitsAbgeholt(spieler.getUniqueId())) {
            plugin.msgs().send(spieler, "daily.already-claimed",
                    "stunden", String.valueOf(plugin.dailyReward().stundenBisMitternacht()));
            return true;
        }
        int serie = plugin.dailyReward().abholen(spieler);
        plugin.msgs().send(spieler, "daily.claimed", "serie", String.valueOf(serie));
        return true;
    }
}
