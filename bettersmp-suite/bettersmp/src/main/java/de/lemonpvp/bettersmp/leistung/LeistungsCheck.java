package de.lemonpvp.bettersmp.leistung;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class LeistungsCheck {

    public record Befund(boolean ok, String wert, String empfehlung) {
    }

    private final BetterSMP plugin;
    private final File serverOrdner;
    private final List<String> jvmArgumente;
    private final long maxSpeicher;

    public LeistungsCheck(BetterSMP plugin) {
        this(plugin, serverOrdner(plugin), ManagementFactory.getRuntimeMXBean().getInputArguments(),
                Runtime.getRuntime().maxMemory());
    }

    public LeistungsCheck(BetterSMP plugin, File serverOrdner, List<String> jvmArgumente, long maxSpeicher) {
        this.plugin = plugin;
        this.serverOrdner = serverOrdner;
        this.jvmArgumente = jvmArgumente;
        this.maxSpeicher = maxSpeicher;
    }

    private static File serverOrdner(BetterSMP plugin) {
        File plugins = plugin.getDataFolder().getAbsoluteFile().getParentFile();
        File oben = plugins == null ? null : plugins.getParentFile();
        return oben != null ? oben : new File(".").getAbsoluteFile();
    }

    private Properties eigenschaften() {
        Properties werte = new Properties();
        File datei = new File(serverOrdner, "server.properties");
        if (datei.isFile()) {
            try (Reader leser = Files.newBufferedReader(datei.toPath(), StandardCharsets.UTF_8)) {
                werte.load(leser);
            } catch (IOException ignoriert) {
            }
        }
        return werte;
    }

    private YamlConfiguration paper(String name) {
        File datei = new File(new File(serverOrdner, "config"), name);
        return datei.isFile() ? YamlConfiguration.loadConfiguration(datei) : null;
    }

    private static int zahl(Properties werte, String schluessel, int standard) {
        try {
            return Integer.parseInt(werte.getProperty(schluessel, String.valueOf(standard)).trim());
        } catch (NumberFormatException falsch) {
            return standard;
        }
    }

    public List<Befund> pruefen() {
        List<Befund> befunde = new ArrayList<>();
        Properties werte = eigenschaften();
        int sicht = zahl(werte, "view-distance", Bukkit.getViewDistance());
        befunde.add(new Befund(sicht <= 10, "view-distance=" + sicht,
                "8 bis 10 - jeder Chunk mehr kostet bei vielen Spielern spuerbar (server.properties)"));
        int simulation = zahl(werte, "simulation-distance", Bukkit.getSimulationDistance());
        befunde.add(new Befund(simulation <= 8, "simulation-distance=" + simulation,
                "6 - so weit laufen Mobs, Pflanzen und Farmen um jeden Spieler (server.properties)"));
        YamlConfiguration global = paper("paper-global.yml");
        boolean proxy = global != null && global.getBoolean("proxies.velocity.enabled", false);
        int kompression = zahl(werte, "network-compression-threshold", 256);
        if (proxy) {
            befunde.add(new Befund(kompression < 0, "network-compression-threshold=" + kompression,
                    "-1 - hinter Velocity auf demselben Rechner komprimiert schon der Proxy (server.properties)"));
        }
        String schreiben = werte.getProperty("sync-chunk-writes", "true").trim();
        befunde.add(new Befund(schreiben.equalsIgnoreCase("false"), "sync-chunk-writes=" + schreiben,
                "false - Chunks werden dann im Hintergrund gespeichert (server.properties)"));
        YamlConfiguration welt = paper("paper-world-defaults.yml");
        if (welt != null) {
            String redstone = welt.getString("misc.redstone-implementation", "VANILLA");
            befunde.add(new Befund(!redstone.equalsIgnoreCase("VANILLA"), "redstone-implementation=" + redstone,
                    "ALTERNATE_CURRENT - viel schnelleres Redstone, verhaelt sich fast immer gleich (paper-world-defaults.yml)"));
            boolean explosionen = welt.getBoolean("environment.optimize-explosions", false);
            befunde.add(new Befund(explosionen, "optimize-explosions=" + explosionen,
                    "true - TNT und Creeper rechnen gleich, nur schneller (paper-world-defaults.yml)"));
        }
        long gb = Math.round(maxSpeicher / (1024.0 * 1024 * 1024));
        befunde.add(new Befund(gb >= 6, "Arbeitsspeicher " + gb + " GB",
                "mindestens 6 GB fuer einen SMP mit vielen Spielern (-Xmx im Startbefehl)"));
        String xms = null;
        String xmx = null;
        boolean g1 = false;
        boolean abgestimmt = false;
        for (String argument : jvmArgumente) {
            if (argument.startsWith("-Xms")) {
                xms = argument.substring(4);
            } else if (argument.startsWith("-Xmx")) {
                xmx = argument.substring(4);
            } else if (argument.equals("-XX:+UseG1GC") || argument.equals("-XX:+UseZGC")) {
                g1 = true;
            } else if (argument.startsWith("-XX:MaxGCPauseMillis") || argument.startsWith("-XX:+ZGenerational")
                    || argument.startsWith("-Daikars.new.flags")) {
                abgestimmt = true;
            }
        }
        boolean gleich = xms != null && xms.equalsIgnoreCase(xmx);
        befunde.add(new Befund(gleich, "-Xms" + (xms == null ? "?" : xms) + " / -Xmx" + (xmx == null ? "?" : xmx),
                "-Xms und -Xmx gleich gross - dann muss Java den Speicher nicht mitten im Spiel nachfordern"));
        befunde.add(new Befund(g1 && abgestimmt, g1 ? (abgestimmt ? "Garbage Collector abgestimmt" : "Garbage Collector ohne Feinabstimmung")
                : "Garbage Collector unbekannt", "die Start-Flags aus LEISTUNG.md (Aikar-Flags) - weniger Ruckler durch Speicherbereinigung"));
        return befunde;
    }

    public void melden(CommandSender empfaenger, Sichtweite sichtweite) {
        List<Befund> befunde = pruefen();
        long probleme = befunde.stream().filter(befund -> !befund.ok()).count();
        plugin.msgs().send(empfaenger, "leistung.kopf", "probleme", String.valueOf(probleme));
        for (Befund befund : befunde) {
            if (befund.ok()) {
                plugin.msgs().send(empfaenger, "leistung.ok", "wert", Text.sicher(befund.wert()));
            } else {
                plugin.msgs().send(empfaenger, "leistung.problem", "wert", Text.sicher(befund.wert()),
                        "empfehlung", Text.sicher(befund.empfehlung()));
            }
        }
        if (sichtweite != null) {
            plugin.msgs().send(empfaenger, "leistung.sichtweite",
                    "bedrock", String.valueOf(plugin.getConfig().getInt("leistung.bedrock-sichtweite", 6)),
                    "reduktion", String.valueOf(sichtweite.reduktion()),
                    "mspt", String.format(Locale.GERMANY, "%.1f", Bukkit.getAverageTickTime()));
        }
    }

    public void beimStart() {
        List<Befund> probleme = pruefen().stream().filter(befund -> !befund.ok()).toList();
        if (probleme.isEmpty()) {
            plugin.getLogger().info("Leistungs-Check: alle Server-Einstellungen passen.");
            return;
        }
        plugin.getLogger().warning("Leistungs-Check: " + probleme.size() + " Einstellung(en) bremsen den Server "
                + "(Details mit /bettersmp leistung):");
        for (Befund befund : probleme) {
            plugin.getLogger().warning("  " + befund.wert() + "  ->  empfohlen: " + befund.empfehlung());
        }
    }
}
