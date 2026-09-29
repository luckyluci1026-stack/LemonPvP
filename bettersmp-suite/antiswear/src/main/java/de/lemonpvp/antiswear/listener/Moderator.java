package de.lemonpvp.antiswear.listener;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import de.lemonpvp.antiswear.filter.Treffer;
import de.lemonpvp.antiswear.strikes.Stufe;
import de.lemonpvp.antiswear.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.stream.Collectors;

public final class Moderator {

    private final AntiSwear plugin;

    public Moderator(AntiSwear plugin) {
        this.plugin = plugin;
    }

    public void verarbeiten(Player spieler, ChatPruefung.Ergebnis ergebnis, String ort, String original) {
        if (ergebnis.hinweis() != null && spieler.isOnline()) {
            plugin.msgs().send(spieler, ergebnis.hinweis());
        }
        if (!ergebnis.verstoss()) {
            return;
        }
        int vorher = plugin.strikes().aktuellePunkte(spieler.getUniqueId());
        Optional<Stufe> stufe = ergebnis.punkte() > 0
                ? plugin.strikes().verstoss(spieler.getUniqueId(), ergebnis.punkte()) : Optional.empty();
        int gesamt = ergebnis.punkte() > 0 ? vorher + ergebnis.punkte() : vorher;
        String grund = grund(ergebnis);
        teamHinweis(spieler.getName(), ort, grund, original, gesamt);
        plugin.protokoll().schreiben(plugin.servername(), spieler.getName(), ort, grund,
                ergebnis.punkte() > 0 ? "+" + ergebnis.punkte() + " (" + gesamt + ")" : "", original);
        stufe.ifPresent(ausgeloest -> ausfuehren(spieler, ausgeloest));
    }

    public String grund(ChatPruefung.Ergebnis ergebnis) {
        String name = plugin.msgs().raw("kategorie." + ergebnis.kategorie().name().toLowerCase(java.util.Locale.ROOT));
        if (!ergebnis.treffer().isEmpty()) {
            return name + ": " + ergebnis.treffer().stream().map(Treffer::wort).distinct().collect(Collectors.joining(", "));
        }
        return ergebnis.fund() != null ? name + ": " + ergebnis.fund() : name;
    }

    private void teamHinweis(String spieler, String ort, String grund, String text, int punkte) {
        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            if (empfaenger.hasPermission("antiswear.notify")) {
                plugin.msgs().send(empfaenger, "team-hinweis", "spieler", spieler, "ort", ort, "woerter", grund,
                        "text", text, "punkte", String.valueOf(punkte));
            }
        }
    }

    private void ausfuehren(Player spieler, Stufe stufe) {
        switch (stufe.aktion()) {
            case WARNEN -> {
                if (spieler.isOnline()) {
                    plugin.msgs().send(spieler, "stufe-warnen");
                }
            }
            case STUMMSCHALTEN -> stummschalten(spieler, stufe.dauerMillis(), true);
            case BEFEHL -> {
                String befehl = stufe.befehl().replace("%spieler%", spieler.getName());
                if (!befehl.isBlank()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), befehl);
                }
            }
        }
    }

    public void stummschalten(Player spieler, long dauerMillis, boolean melden) {
        plugin.strikes().stummschalten(spieler.getUniqueId(), dauerMillis);
        plugin.netzwerk().stummMelden(spieler.getUniqueId(), dauerMillis < 0 ? -1 : dauerMillis);
        if (melden && spieler.isOnline()) {
            plugin.msgs().send(spieler, "stufe-stummschalten", "dauer",
                    dauerMillis < 0 ? plugin.msgs().raw("unbegrenzt") : Durations.humanize(dauerMillis));
        }
        plugin.protokoll().schreiben(plugin.servername(), spieler.getName(), "Strafe",
                "stumm " + (dauerMillis < 0 ? "unbegrenzt" : Durations.humanize(dauerMillis)));
    }
}
