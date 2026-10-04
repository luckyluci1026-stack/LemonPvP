package de.lemonpvp.reportplus.command;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.gui.Guis;
import de.lemonpvp.reportplus.util.Kategorie;
import de.lemonpvp.reportplus.util.Ziel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public Optional<Ziel> finde(Player melder, String eingabe) {
        String name = eingabe == null ? "" : eingabe.trim();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online == null && !name.startsWith(".")) {
            online = Bukkit.getPlayerExact("." + name);
        }
        if (online != null) {
            return Optional.of(new Ziel(online.getUniqueId(), online.getName()));
        }
        for (Ziel ziel : plugin.kontakte().letzte(melder.getUniqueId())) {
            if (ziel.heisst(name)) {
                return Optional.of(ziel);
            }
        }
        Optional<Ziel> gegner = plugin.kontakte().finde(name);
        if (gegner.isPresent()) {
            return gegner;
        }
        OfflinePlayer bekannt = Bukkit.getOfflinePlayerIfCached(name);
        if (bekannt == null && !name.startsWith(".")) {
            bekannt = Bukkit.getOfflinePlayerIfCached("." + name);
        }
        if (bekannt == null) {
            return Optional.empty();
        }
        return Optional.of(new Ziel(bekannt.getUniqueId(), bekannt.getName() == null ? name : bekannt.getName()));
    }

    public void starteMitZiel(Player melder, Ziel ziel) {
        if (melder.getUniqueId().equals(ziel.id())) {
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
                plugin.msgs().raw("report.reason-title").replace("%spieler%", ziel.name()),
                kategorien,
                kategorieId -> abschliessen(melder, ziel, kategorieId));
    }

    private void abschliessen(Player melder, Ziel ziel, String kategorieId) {
        letzteMeldung.put(melder.getUniqueId(), System.currentTimeMillis());
        plugin.reports().anlegen(melder.getUniqueId(), melder.getName(), ziel.id(), ziel.name(), kategorieId);
        plugin.msgs().send(melder, "report.submitted", "spieler", ziel.name());
        Bukkit.getPluginManager().callEvent(new de.lemonpvp.reportplus.api.SpielerGemeldetEvent(
                melder.getUniqueId(), melder.getName(), ziel.id(), ziel.name(), kategorieId));

        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            if (empfaenger.hasPermission("bettersmp.report.receive")) {
                plugin.msgs().send(empfaenger, "report.received",
                        "melder", melder.getName(), "spieler", ziel.name(), "grund", kategorieId);
            }
        }
    }
}
