package de.lemonpvp.antiswear.command;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import de.lemonpvp.antiswear.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AntiSwearCommand implements TabExecutor {

    private static final List<String> UNTERBEFEHLE = List.of("reload", "check", "punkte", "reset", "stumm");

    private final AntiSwear plugin;

    public AntiSwearCommand(AntiSwear plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            plugin.msgs().send(sender, "hilfe");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.ladeAlles();
                plugin.msgs().send(sender, "reloaded");
            }
            case "check" -> pruefeText(sender, args);
            case "punkte" -> zeigePunkte(sender, args);
            case "reset" -> setzeZurueck(sender, args);
            case "stumm" -> stummschalten(sender, args);
            default -> plugin.msgs().send(sender, "hilfe");
        }
        return true;
    }

    private void pruefeText(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msgs().send(sender, "check-usage");
            return;
        }
        String text = String.join(" ", Arrays.asList(args).subList(1, args.length));
        ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefenOhneSpam(text);
        if (!ergebnis.verstoss()) {
            plugin.msgs().send(sender, "check-sauber", "text", ergebnis.text());
            return;
        }
        plugin.msgs().send(sender, "check-treffer",
                "woerter", plugin.moderator().grund(ergebnis), "punkte", String.valueOf(ergebnis.punkte()),
                "zensiert", ergebnis.aktion() == ChatPruefung.Aktion.BLOCKIERT ? plugin.msgs().raw("check-blockiert") : ergebnis.text());
    }

    private void zeigePunkte(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msgs().send(sender, "punkte-usage");
            return;
        }
        OfflinePlayer ziel = spieler(args[1]);
        int punkte = plugin.strikes().aktuellePunkte(ziel.getUniqueId());
        long rest = plugin.strikes().stummRestMillis(ziel.getUniqueId());
        plugin.msgs().send(sender, "punkte-anzeige", "spieler", args[1], "punkte", String.valueOf(punkte),
                "stumm", rest == 0 ? "-" : rest < 0 ? plugin.msgs().raw("unbegrenzt") : Durations.humanize(rest));
    }

    private void setzeZurueck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msgs().send(sender, "reset-usage");
            return;
        }
        OfflinePlayer ziel = spieler(args[1]);
        plugin.strikes().zuruecksetzen(ziel.getUniqueId());
        plugin.netzwerk().stummMelden(ziel.getUniqueId(), 0);
        plugin.msgs().send(sender, "reset-ok", "spieler", args[1]);
        plugin.protokoll().schreiben(plugin.servername(), args[1], "Team", "zurueckgesetzt von " + sender.getName());
    }

    private void stummschalten(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msgs().send(sender, "stumm-usage");
            return;
        }
        Player ziel = Bukkit.getPlayerExact(args[1]);
        if (ziel == null) {
            plugin.msgs().send(sender, "nicht-online", "spieler", args[1]);
            return;
        }
        String dauerText = args[2].toLowerCase(Locale.ROOT);
        long dauer = dauerText.startsWith("perm") ? -1 : Durations.parse(dauerText);
        if (dauer == 0) {
            plugin.msgs().send(sender, "stumm-usage");
            return;
        }
        plugin.moderator().stummschalten(ziel, dauer, true);
        plugin.msgs().send(sender, "stumm-ok", "spieler", ziel.getName(),
                "dauer", dauer < 0 ? plugin.msgs().raw("unbegrenzt") : Durations.humanize(dauer));
    }

    private static OfflinePlayer spieler(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer bekannt = Bukkit.getOfflinePlayerIfCached(name);
        return bekannt != null ? bekannt : Bukkit.getOfflinePlayer(name);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return UNTERBEFEHLE.stream().filter(u -> u.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && List.of("punkte", "reset", "stumm").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> namen = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    namen.add(online.getName());
                }
            }
            return namen;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stumm")) {
            return List.of("10m", "1h", "1d", "perm");
        }
        return List.of();
    }
}
