package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /gban /gunban /gmute /gunmute - ein Executor fuer alle vier Befehle.
 */
public final class PunishmentCommands implements TabExecutor {

    private final BetterSMP plugin;

    public PunishmentCommands(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        boolean banCmd = name.equals("gban") || name.equals("gunban");
        String perm = banCmd ? "bettersmp.ban" : "bettersmp.mute";
        if (!sender.hasPermission(perm)) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }

        switch (name) {
            case "gban" -> {
                if (args.length < 2) {
                    plugin.msgs().send(sender, "ban.usage");
                    return true;
                }
                resolveThen(args[0], target -> plugin.punishments().ban(sender, target, args[1]));
            }
            case "gunban" -> {
                if (args.length < 1) {
                    plugin.msgs().send(sender, "ban.unban-usage");
                    return true;
                }
                resolveThen(args[0], target -> plugin.punishments().unban(sender, target));
            }
            case "gmute" -> {
                if (args.length < 2) {
                    plugin.msgs().send(sender, "mute.usage");
                    return true;
                }
                resolveThen(args[0], target -> plugin.punishments().mute(sender, target, args[1]));
            }
            case "gunmute" -> {
                if (args.length < 1) {
                    plugin.msgs().send(sender, "mute.unmute-usage");
                    return true;
                }
                resolveThen(args[0], target -> plugin.punishments().unmute(sender, target));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Loest den Spielernamen asynchron auf (kein Blockieren des Main-Threads). */
    private void resolveThen(String name, java.util.function.Consumer<OfflinePlayer> action) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            action.accept(online);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(name);
            }
            action.accept(target);
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 2 && name.equals("gban")) {
            addMatching(out, plugin.punishments().config().banReasonKeys(), args[1]);
        } else if (args.length == 2 && name.equals("gmute")) {
            addMatching(out, plugin.punishments().config().muteReasonKeys(), args[1]);
        }
        return out;
    }

    private void addMatching(List<String> out, java.util.Set<String> keys, String arg) {
        String prefix = arg.toLowerCase(Locale.ROOT);
        for (String key : keys) {
            if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(key);
            }
        }
    }
}
