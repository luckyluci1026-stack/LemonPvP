package de.lemonpvp.bettersmp.stats;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.storage.StatSnapshot;
import de.lemonpvp.bettersmp.util.Durations;
import de.lemonpvp.bettersmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /stats [Spieler]
 */
public final class StatsCommand implements CommandExecutor {

    private final BetterSMP plugin;

    public StatsCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1) {
            Player online = Bukkit.getPlayerExact(args[0]);
            if (online != null) {
                show(sender, online.getUniqueId(), online.getName(), online);
                return true;
            }
            plugin.msgs().send(sender, "stats.loading");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
                if (target == null) {
                    target = Bukkit.getOfflinePlayer(args[0]);
                }
                show(sender, target.getUniqueId(),
                        target.getName() != null ? target.getName() : args[0], null);
            });
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        show(sender, player.getUniqueId(), player.getName(), player);
        return true;
    }

    private void show(CommandSender to, UUID uuid, String name, Player onlineOrNull) {
        plugin.database().getStats(uuid).thenAccept(snapshot ->
                Bukkit.getScheduler().runTask(plugin, () -> render(to, name, snapshot, onlineOrNull)));
    }

    private void render(CommandSender to, String name, StatSnapshot s, Player onlineOrNull) {
        String rank = onlineOrNull != null && plugin.luckPerms().isAvailable()
                ? Text.legacyToMini(plugin.luckPerms().prefix(onlineOrNull)) + "<white>" + name
                : "<gray>-";
        String money = plugin.economy().isEnabled() && onlineOrNull != null
                ? plugin.economy().format(plugin.economy().balance(onlineOrNull))
                : (plugin.economy().isEnabled()
                    ? plugin.economy().format(plugin.economy().balance(Bukkit.getOfflinePlayer(s.uuid())))
                    : "-");

        to.sendMessage(plugin.msgs().format("stats.header", "player", name));
        to.sendMessage(plugin.msgs().format("stats.line-rank", "rank", rank));
        to.sendMessage(plugin.msgs().format("stats.line-money", "money", money));
        to.sendMessage(plugin.msgs().format("stats.line-kills",
                "kills", String.valueOf(s.kills()),
                "deaths", String.valueOf(s.deaths()),
                "kd", String.valueOf(s.kd())));
        to.sendMessage(plugin.msgs().format("stats.line-mobkills",
                "mobkills", String.valueOf(s.mobKills())));
        to.sendMessage(plugin.msgs().format("stats.line-playtime",
                "playtime", s.playtime() <= 0 ? "0m" : Durations.humanize(s.playtime() * 1000L)));
        to.sendMessage(plugin.msgs().format("stats.line-joins",
                "joins", String.valueOf(s.joins()),
                "lastseen", Durations.expiry(s.lastSeen(), "-")));
    }
}
