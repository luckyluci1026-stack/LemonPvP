package de.lemonpvp.antiswear.voice;

import de.lemonpvp.antiswear.AntiSwear;
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
import java.util.Map;
import java.util.UUID;

public final class VoiceBefehle implements TabExecutor {

    private static final int MAX_GRUND = 120;
    private static final List<String> DAUERN = List.of("10m", "30m", "1h", "1d", "7d", "perm");
    private static final List<String> ZUSTIMMEN = List.of("akzeptieren", "accept", "ja");

    private final AntiSwear plugin;

    public VoiceBefehle(AntiSwear plugin) {
        this.plugin = plugin;
    }

    private VoiceModeration voice() {
        return plugin.voice();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "vcrules" -> regeln(sender, args);
            case "vcmute" -> stumm(sender, args);
            case "vcunmute" -> aufheben(sender, args);
            case "vcmutes" -> liste(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void regeln(CommandSender sender, String[] args) {
        if (args.length > 0 && ZUSTIMMEN.contains(args[0].toLowerCase(Locale.ROOT))) {
            if (sender instanceof Player p) {
                voice().zustimmen(p);
            } else {
                plugin.msgs().send(sender, "voice.nur-spieler");
            }
            return;
        }
        voice().regelnZeigen(sender);
    }

    private void stumm(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msgs().send(sender, "voice.stumm-usage");
            return;
        }
        OfflinePlayer ziel = finden(args[0]);
        if (ziel == null) {
            plugin.msgs().send(sender, "voice.unbekannt", "spieler", args[0]);
            return;
        }
        String dauerText = args[1].toLowerCase(Locale.ROOT);
        long bis;
        if (dauerText.startsWith("perm")) {
            bis = VoiceDaten.FUER_IMMER;
        } else {
            long dauer = Durations.parse(dauerText);
            if (dauer <= 0) {
                plugin.msgs().send(sender, "voice.stumm-usage");
                return;
            }
            bis = System.currentTimeMillis() + dauer;
        }
        String grund = args.length > 2 ? String.join(" ", Arrays.asList(args).subList(2, args.length)).trim() : "";
        if (grund.isEmpty()) {
            grund = plugin.msgs().raw("voice.kein-grund");
        }
        if (grund.length() > MAX_GRUND) {
            grund = grund.substring(0, MAX_GRUND);
        }
        String name = ziel.getName() == null ? args[0] : ziel.getName();
        voice().stummschalten(sender, ziel.getUniqueId(), name, bis, grund);
        plugin.msgs().send(sender, "voice.stumm-ok", "spieler", name, "dauer", voice().rest(bis));
    }

    private void aufheben(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.msgs().send(sender, "voice.entstummt-usage");
            return;
        }
        UUID id = null;
        String name = args[0];
        for (Map.Entry<UUID, VoiceDaten.Eintrag> eintrag
                : voice().daten().aktiveStummschaltungen(System.currentTimeMillis()).entrySet()) {
            if (eintrag.getValue().name().equalsIgnoreCase(args[0])) {
                id = eintrag.getKey();
                name = eintrag.getValue().name();
            }
        }
        if (id == null) {
            OfflinePlayer ziel = finden(args[0]);
            if (ziel != null) {
                id = ziel.getUniqueId();
                name = ziel.getName() == null ? args[0] : ziel.getName();
            }
        }
        if (id == null || !voice().aufheben(sender, id, name)) {
            plugin.msgs().send(sender, "voice.nicht-stumm", "spieler", name);
            return;
        }
        plugin.msgs().send(sender, "voice.entstummt-ok", "spieler", name);
    }

    private void liste(CommandSender sender) {
        Map<UUID, VoiceDaten.Eintrag> aktiv = voice().daten().aktiveStummschaltungen(System.currentTimeMillis());
        if (aktiv.isEmpty()) {
            plugin.msgs().send(sender, "voice.liste-leer");
            return;
        }
        plugin.msgs().send(sender, "voice.liste-kopf", "anzahl", String.valueOf(aktiv.size()));
        for (VoiceDaten.Eintrag eintrag : aktiv.values()) {
            plugin.msgs().send(sender, "voice.liste-eintrag", "spieler", eintrag.name(), "rest", voice().rest(eintrag.wert()),
                    "von", eintrag.von().isEmpty() ? "?" : eintrag.von(),
                    "grund", eintrag.grund().isEmpty() ? plugin.msgs().raw("voice.kein-grund") : eintrag.grund());
        }
    }

    private static OfflinePlayer finden(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("vcrules")) {
            return args.length == 1 ? passend(List.of("akzeptieren"), args[0]) : List.of();
        }
        if (name.equals("vcmute")) {
            if (args.length == 1) {
                return passend(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
            }
            return args.length == 2 ? passend(DAUERN, args[1]) : List.of();
        }
        if (name.equals("vcunmute") && args.length == 1) {
            return passend(voice().daten().aktiveStummschaltungen(System.currentTimeMillis()).values().stream()
                    .map(VoiceDaten.Eintrag::name).toList(), args[0]);
        }
        return List.of();
    }

    private static List<String> passend(List<String> werte, String anfang) {
        String klein = anfang.toLowerCase(Locale.ROOT);
        List<String> ergebnis = new ArrayList<>();
        for (String wert : werte) {
            if (wert.toLowerCase(Locale.ROOT).startsWith(klein)) {
                ergebnis.add(wert);
            }
        }
        return ergebnis;
    }
}
