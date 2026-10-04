package de.lemonpvp.bettersmp.report;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
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
    private final LetzteGegner letzteGegner = new LetzteGegner();
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

    public LetzteGegner letzteGegner() {
        return letzteGegner;
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
        Optional<Ziel> gegner = letzteGegner.finde(melder.getUniqueId(), name);
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

    public void vermerken(Player melder, Ziel gemeldet, String grund) {
        letzteMeldung.put(melder.getUniqueId(), System.currentTimeMillis());
        schreibeProtokoll(melder, gemeldet, grund);
    }

    private void schreibeProtokoll(Player melder, Ziel gemeldet, String grund) {
        File datei = new File(plugin.getDataFolder(), "reports.log");
        Player online = Bukkit.getPlayer(gemeldet.id());
        String zeile = String.format("[%s] %s meldet %s (Welt: %s): %s%n",
                LocalDateTime.now().format(ZEITSTEMPEL),
                melder.getName(), gemeldet.name(),
                online == null ? "nicht auf diesem Server" : online.getWorld().getName(), grund);
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
