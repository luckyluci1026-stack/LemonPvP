package de.lemonpvp.duelplus.command;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /duel <Spieler> | /duel accept <Spieler> | /duel decline <Spieler> */
public final class DuelCommand implements TabExecutor {

    private final DuelPlus plugin;

    public DuelCommand(DuelPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "player-only");
            return true;
        }
        if (!spieler.hasPermission("duelplus.use")) {
            plugin.msgs().send(spieler, "no-permission");
            return true;
        }
        if (!plugin.db().bereit()) {
            plugin.msgs().send(spieler, "not-ready");
            return true;
        }
        if (args.length < 1) {
            plugin.msgs().send(spieler, "usage");
            return true;
        }
        String erstesArgument = args[0].toLowerCase(Locale.ROOT);
        if (erstesArgument.equals("accept") || erstesArgument.equals("decline")) {
            if (args.length < 2) {
                plugin.msgs().send(spieler, "usage");
                return true;
            }
            if (erstesArgument.equals("accept")) {
                plugin.anfragen().annehmen(spieler, args[1]);
            } else {
                plugin.anfragen().ablehnen(spieler, args[1]);
            }
            return true;
        }
        plugin.anfragen().anfordern(spieler, args[0]);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> vorschlaege = new ArrayList<>(List.of("accept", "decline"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                vorschlaege.add(online.getName());
            }
            return vorschlaege;
        }
        return List.of();
    }
}
