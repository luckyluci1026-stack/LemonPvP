package de.lemonpvp.bettersmp.xray;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class XrayBefehl implements CommandExecutor, TabCompleter {

    private static final int LISTE = 8;
    private static final DateTimeFormatter UHRZEIT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

    private final BetterSMP plugin;
    private final XrayAlarm alarm;

    public XrayBefehl(BetterSMP plugin, XrayAlarm alarm) {
        this.plugin = plugin;
        this.alarm = alarm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bettersmp.xray")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            liste(sender);
            return true;
        }
        XrayAlarm.Verlauf verlauf = alarm.verlauf(args[0]);
        if (verlauf == null) {
            plugin.msgs().send(sender, "xray.keine-daten", "player", args[0].replaceAll("[^A-Za-z0-9_.]", ""));
            return true;
        }
        details(sender, verlauf);
        return true;
    }

    private void details(CommandSender sender, XrayAlarm.Verlauf verlauf) {
        long jetzt = System.currentTimeMillis();
        plugin.msgs().send(sender, "xray.kopf", "player", verlauf.name,
                "seit", UHRZEIT.format(Instant.ofEpochMilli(verlauf.beginn).atZone(ZoneId.systemDefault())));
        plugin.msgs().send(sender, "xray.zeile-bloecke", "bloecke", String.format(Locale.GERMANY, "%,d", verlauf.bloeckeGesamt));
        verlauf.arten.forEach((art, zaehler) -> plugin.msgs().send(sender, "xray.zeile-art", "art", art,
                "anzahl", String.valueOf(zaehler[0]), "versteckt", String.valueOf(zaehler[1])));
        if (verlauf.versteckteGesamt > 0) {
            plugin.msgs().send(sender, "xray.zeile-schnitt", "schnitt", String.valueOf(schnitt(verlauf)),
                    "grenze", String.valueOf(alarm.grenze()));
        }
        int fenster = alarm.fenster();
        int funde = alarm.versteckteSeit(verlauf, jetzt - fenster * 60_000L, new LinkedHashMap<>());
        plugin.msgs().send(sender, "xray.zeile-fenster", "minuten", String.valueOf(fenster), "funde", String.valueOf(funde),
                "bloecke", String.valueOf(XrayAlarm.bloeckeImFenster(verlauf, jetzt, fenster)));
        String letzter = verlauf.letzterAlarm == 0 ? plugin.msgs().raw("xray.kein-alarm")
                : plugin.msgs().raw("xray.zeit-vor").replace("%minuten%",
                String.valueOf(Math.max(0, (jetzt - verlauf.letzterAlarm) / 60_000L)));
        plugin.msgs().send(sender, "xray.zeile-alarm", "alarm", letzter);
    }

    private void liste(CommandSender sender) {
        List<XrayAlarm.Verlauf> auffaellig = new ArrayList<>();
        for (XrayAlarm.Verlauf verlauf : alarm.alle()) {
            if (verlauf.versteckteGesamt >= 2) {
                auffaellig.add(verlauf);
            }
        }
        if (auffaellig.isEmpty()) {
            plugin.msgs().send(sender, "xray.liste-leer");
            return;
        }
        auffaellig.sort(Comparator.comparingLong(XrayBefehl::schnitt).thenComparing(v -> -v.versteckteGesamt));
        plugin.msgs().send(sender, "xray.liste-kopf");
        for (XrayAlarm.Verlauf verlauf : auffaellig.subList(0, Math.min(LISTE, auffaellig.size()))) {
            plugin.msgs().send(sender, "xray.liste-zeile", "player", verlauf.name,
                    "funde", String.valueOf(verlauf.versteckteGesamt), "schnitt", String.valueOf(schnitt(verlauf)));
        }
    }

    static long schnitt(XrayAlarm.Verlauf verlauf) {
        return verlauf.bloeckeGesamt / Math.max(1, verlauf.versteckteGesamt);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> vorschlaege = new ArrayList<>();
        if (args.length != 1 || !sender.hasPermission("bettersmp.xray")) {
            return vorschlaege;
        }
        String anfang = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> namen = new LinkedHashMap<>();
        for (XrayAlarm.Verlauf verlauf : alarm.alle()) {
            namen.putIfAbsent(verlauf.name.toLowerCase(Locale.ROOT), verlauf.name);
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            namen.putIfAbsent(online.getName().toLowerCase(Locale.ROOT), online.getName());
        }
        namen.forEach((klein, name) -> {
            if (klein.startsWith(anfang)) {
                vorschlaege.add(name);
            }
        });
        return vorschlaege;
    }
}
