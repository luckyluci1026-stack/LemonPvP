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

/** Ablauf fuer /bugreport: Kategorie per GUI auswaehlen, Text per Chat eintippen. */
public final class BugFlow {

    private final ReportPlus plugin;
    private final Map<UUID, String> wartetAufText = new ConcurrentHashMap<>();
    private final Map<UUID, Long> letzteMeldung = new ConcurrentHashMap<>();

    public BugFlow(ReportPlus plugin) {
        this.plugin = plugin;
    }

    public void starte(Player spieler) {
        long cooldownMs = plugin.getConfig().getLong("bugreport.cooldown-seconds", 30) * 1000L;
        Long letzte = letzteMeldung.get(spieler.getUniqueId());
        if (letzte != null && System.currentTimeMillis() - letzte < cooldownMs) {
            long rest = (cooldownMs - (System.currentTimeMillis() - letzte)) / 1000L + 1;
            plugin.msgs().send(spieler, "bugreport.cooldown", "sekunden", String.valueOf(rest));
            return;
        }
        List<Kategorie> kategorien = Kategorie.laden(plugin, "bugreport.categories");
        Guis.oeffneKategoriePicker(spieler, plugin.msgs().raw("bugreport.category-title"),
                kategorien, kategorieId -> aufTextWarten(spieler, kategorieId));
    }

    private void aufTextWarten(Player spieler, String kategorieId) {
        wartetAufText.put(spieler.getUniqueId(), kategorieId);
        plugin.msgs().send(spieler, "bugreport.awaiting-text");
    }

    /** Peek, ob gerade auf Chattext von dieser Person gewartet wird - fuer den Listener, um rechtzeitig abzufangen. */
    public boolean wartetAuf(UUID spielerId) {
        return wartetAufText.containsKey(spielerId);
    }

    /** Verarbeitet die abgefangene Chatzeile als Bugmeldungstext (oder Abbruch). */
    public void empfangeText(Player spieler, String text) {
        String kategorieId = wartetAufText.remove(spieler.getUniqueId());
        if (kategorieId == null) {
            return;
        }
        if (text.equalsIgnoreCase("abbrechen")) {
            plugin.msgs().send(spieler, "bugreport.cancelled");
            return;
        }
        letzteMeldung.put(spieler.getUniqueId(), System.currentTimeMillis());
        plugin.bugs().anlegen(spieler.getUniqueId(), spieler.getName(), kategorieId, text);
        plugin.msgs().send(spieler, "bugreport.submitted");

        for (Player empfaenger : Bukkit.getOnlinePlayers()) {
            if (empfaenger.hasPermission("bettersmp.bugreport.receive")) {
                plugin.msgs().send(empfaenger, "bugreport.received",
                        "spieler", spieler.getName(), "kategorie", kategorieId, "text", text);
            }
        }
    }
}
