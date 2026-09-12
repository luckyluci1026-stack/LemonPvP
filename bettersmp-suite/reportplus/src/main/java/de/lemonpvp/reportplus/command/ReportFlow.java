package de.lemonpvp.reportplus.command;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.gui.Guis;
import de.lemonpvp.reportplus.util.Kategorie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gemeinsame Logik fuer /report - egal ob per Klick im Spieler-Picker
 * oder per getipptem Namen gestartet.
 */
public final class ReportFlow {

    private final ReportPlus plugin;
    private final Map<UUID, Long> letzteMeldung = new ConcurrentHashMap<>();

    public ReportFlow(ReportPlus plugin) {
        this.plugin = plugin;
    }

    public void starteMitZiel(Player melder, UUID zielId) {
        Player ziel = Bukkit.getPlayer(zielId);
        if (ziel == null) {
            plugin.msgs().send(melder, "report.not-online", "spieler", zielId.toString());
            return;
        }
        starteMitZiel(melder, ziel);
    }

    public void starteMitZiel(Player melder, Player ziel) {
        if (melder.getUniqueId().equals(ziel.getUniqueId())) {
            plugin.msgs().send(melder, "report.not-yourself");
            return;
        }
        long cooldownMs = plugin.getConfig().getLong("report.cooldown-seconds", 60) * 1000L;
        Long letzte = letzteMeldung.get(melder.getUniqueId());
        if (letzte != null && System.currentTimeMillis() - letzte < cooldownMs) {
            long rest = (cooldownMs - (System.currentTimeMillis() - letzte)) / 1000L + 1;
            plugin.msgs().send(melder, "report.cooldown", "sekunden", String.valueOf(rest));
            return;
        }

        List<Kategorie> kategorien = Kategorie.laden(plugin, "report.categories");
        Guis.oeffneKategoriePicker(melder,
                plugin.msgs().raw("report.reason-title").replace("%spieler%", ziel.getName()),
                kategorien,
                kategorieId -> abschliessen(melder, ziel, kategorieId));
    }

    private void abschliessen(Player melder, Player ziel, String kategorieId) {
        letzteMeldung.put(melder.getUniqueId(), System.currentTimeMillis());
        plugin.reports().anlegen(melder.getUniqueId(), melder.getName(), ziel.getUniqueId(), ziel.getName(), kategorieId);
        plugin.msgs().send(melder, "report.submitted", "spieler", ziel.getName());

        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            if (empfaenger.hasPermission("bettersmp.report.receive")) {
                plugin.msgs().send(empfaenger, "report.received",
                        "melder", melder.getName(), "spieler", ziel.getName(), "grund", kategorieId);
            }
        }
    }
}
