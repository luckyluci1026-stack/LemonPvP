package de.lemonpvp.duelplus.command;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.StatEintrag;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /duel <Spieler> [accept|decline] | /duel stats [Spieler] | /duel top [Anzahl] | /duel watch <Spieler> | /duel unwatch */
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
        if (erstesArgument.equals("stats")) {
            Player ziel = args.length >= 2 ? Bukkit.getPlayer(args[1]) : spieler;
            if (ziel == null) {
                plugin.msgs().send(spieler, "target-offline", "spieler", args[1]);
                return true;
            }
            statistikZeigen(spieler, ziel.getUniqueId(), ziel.getName());
            return true;
        }
        if (erstesArgument.equals("top")) {
            int anzahl = 10;
            if (args.length >= 2) {
                try {
                    anzahl = Math.max(1, Math.min(15, Integer.parseInt(args[1])));
                } catch (NumberFormatException ignored) {
                    // ungueltige Zahl - beim Standardwert bleiben statt Fehlermeldung
                }
            }
            ranglisteZeigen(spieler, anzahl);
            return true;
        }
        if (erstesArgument.equals("watch")) {
            if (args.length < 2) {
                plugin.msgs().send(spieler, "usage");
                return true;
            }
            plugin.zuschauer().anfordern(spieler, args[1]);
            return true;
        }
        if (erstesArgument.equals("unwatch")) {
            plugin.zuschauer().beenden(spieler);
            return true;
        }
        // Ab hier ist args[0] der Zielspieler (der Herausforderer bei accept/
        // decline, oder das Ziel einer neuen Herausforderung) - "Zielspieler
        // zuerst" wie schon beim reinen Herausfordern, statt wie frueher
        // Verb-zuerst nur bei accept/decline.
        if (args.length >= 2) {
            String zweitesArgument = args[1].toLowerCase(Locale.ROOT);
            if (zweitesArgument.equals("accept")) {
                plugin.anfragen().annehmen(spieler, args[0]);
                return true;
            }
            if (zweitesArgument.equals("decline")) {
                plugin.anfragen().ablehnen(spieler, args[0]);
                return true;
            }
        }
        plugin.anfragen().anfordern(spieler, args[0]);
        return true;
    }

    private void statistikZeigen(Player sender, UUID ziel, String zielName) {
        plugin.db().statistikVon(ziel).thenAccept(statOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
            StatEintrag stat = statOpt.orElse(new StatEintrag(ziel, zielName, 0, 0, 0));
            plugin.msgs().send(sender, "stats-anzeige",
                    "spieler", zielName,
                    "siege", String.valueOf(stat.siege()),
                    "niederlagen", String.valueOf(stat.niederlagen()),
                    "unentschieden", String.valueOf(stat.unentschieden()),
                    "quote", String.valueOf(stat.siegquote()));
        }));
    }

    private void ranglisteZeigen(Player sender, int anzahl) {
        plugin.db().rangliste(anzahl).thenAccept(liste -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (liste.isEmpty()) {
                plugin.msgs().send(sender, "top-leer");
                return;
            }
            plugin.msgs().send(sender, "top-kopf");
            int platz = 1;
            for (StatEintrag stat : liste) {
                plugin.msgs().send(sender, "top-zeile",
                        "platz", String.valueOf(platz++),
                        "spieler", stat.name(),
                        "siege", String.valueOf(stat.siege()),
                        "niederlagen", String.valueOf(stat.niederlagen()));
            }
        }));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> vorschlaege = new ArrayList<>(List.of("stats", "top", "watch", "unwatch"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                vorschlaege.add(online.getName());
            }
            return vorschlaege;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("watch"))) {
            List<String> vorschlaege = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                vorschlaege.add(online.getName());
            }
            return vorschlaege;
        }
        // args[0] ist hier weder stats/top/watch/unwatch - also ein
        // Zielspieler, per "/duel <Spieler> accept|decline" (siehe onCommand).
        if (args.length == 2 && !args[0].equalsIgnoreCase("top") && !args[0].equalsIgnoreCase("unwatch")) {
            return List.of("accept", "decline");
        }
        return List.of();
    }
}
