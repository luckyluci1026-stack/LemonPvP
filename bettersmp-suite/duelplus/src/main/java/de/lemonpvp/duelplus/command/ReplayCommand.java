package de.lemonpvp.duelplus.command;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.replay.ReplayManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReplayCommand implements TabExecutor {

    private static final List<String> UNTERBEFEHLE = List.of("liste", "ansehen", "stop", "pause", "tempo", "springen",
            "behalten", "speicher");

    private final DuelPlus plugin;

    public ReplayCommand(DuelPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(ReplayManager.RECHT)) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (!plugin.db().bereit()) {
            plugin.msgs().send(sender, "not-ready");
            return true;
        }
        ReplayManager replays = plugin.replays();
        String unter = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
        String wert = args.length >= 2 ? args[1] : null;
        switch (unter) {
            case "liste", "list" -> replays.liste(sender, wert);
            case "ansehen", "play", "view" -> {
                if (!(sender instanceof Player spieler)) {
                    plugin.msgs().send(sender, "player-only");
                } else if (wert == null) {
                    plugin.msgs().send(sender, "replay-hilfe");
                } else {
                    replays.ansehen(spieler, wert.startsWith("#") ? wert.substring(1) : wert);
                }
            }
            case "stop", "pause", "weiter", "tempo", "springen" -> {
                if (sender instanceof Player spieler) {
                    replays.steuern(spieler, unter, wert);
                } else {
                    plugin.msgs().send(sender, "player-only");
                }
            }
            case "behalten", "keep" -> {
                if (wert == null) {
                    plugin.msgs().send(sender, "replay-hilfe");
                } else {
                    replays.behalten(sender, wert.startsWith("#") ? wert.substring(1) : wert);
                }
            }
            case "speicher" -> replays.speicherZeigen(sender);
            default -> replays.liste(sender, args[0]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(ReplayManager.RECHT)) {
            return List.of();
        }
        if (args.length == 1) {
            return passend(UNTERBEFEHLE, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "liste" -> passend(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
                case "tempo" -> passend(List.of("0.25", "0.5", "1", "2", "4"), args[1]);
                case "springen" -> passend(List.of("-10", "10", "-30", "30", "0:00"), args[1]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<String> passend(List<String> werte, String anfang) {
        List<String> ergebnis = new ArrayList<>();
        for (String wert : werte) {
            if (wert.toLowerCase(Locale.ROOT).startsWith(anfang.toLowerCase(Locale.ROOT))) {
                ergebnis.add(wert);
            }
        }
        return ergebnis;
    }
}
