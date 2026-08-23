package de.lemonpvp.bettersmp.report;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meldungen von Spielern an das Team.
 *
 * Das Protokoll ist bewusst eine einfache Textdatei und keine
 * Datenbanktabelle: Meldungen liest sich ein Lehrer schneller in einem
 * Editor durch, als dass er dafuer ein Admin-Tool braeuchte - und es
 * gibt keine Datenbankstruktur, die dafuer extra gepflegt werden muesste.
 */
public final class ReportManager {

    private final BetterSMP plugin;
    private final Map<UUID, Long> letzteMeldung = new ConcurrentHashMap<>();
    private static final DateTimeFormatter ZEITSTEMPEL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportManager(BetterSMP plugin) {
        this.plugin = plugin;
    }

    /** @return verbleibende Sekunden bis zur naechsten Meldung, 0 wenn sie jetzt darf */
    public long verbleibendeSekunden(UUID melder) {
        long cooldownMillis = plugin.getConfig().getLong("report.cooldown-seconds", 60) * 1000L;
        Long letzte = letzteMeldung.get(melder);
        if (letzte == null) {
            return 0;
        }
        long rest = cooldownMillis - (System.currentTimeMillis() - letzte);
        return Math.max(0, (rest + 999) / 1000);
    }

    public void vermerken(Player melder, Player gemeldet, String grund) {
        letzteMeldung.put(melder.getUniqueId(), System.currentTimeMillis());
        schreibeProtokoll(melder, gemeldet, grund);
    }

    private void schreibeProtokoll(Player melder, Player gemeldet, String grund) {
        File datei = new File(plugin.getDataFolder(), "reports.log");
        String zeile = String.format("[%s] %s meldet %s (Welt: %s): %s%n",
                LocalDateTime.now().format(ZEITSTEMPEL),
                melder.getName(), gemeldet.getName(),
                gemeldet.getWorld().getName(), grund);
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            try (FileWriter schreiber = new FileWriter(datei, true)) {
                schreiber.write(zeile);
            }
        } catch (IOException fehler) {
            plugin.getLogger().warning("reports.log liess sich nicht schreiben: " + fehler.getMessage());
        }
    }
}
