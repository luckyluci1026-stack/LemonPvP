package de.lemonpvp.easybedrock.setup;

import de.lemonpvp.easybedrock.EasyBedrock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optimiert die Geyser-Config ressourcenschonend - per Text-Patch, damit
 * Kommentare und alle uebrigen Werte erhalten bleiben. Es werden nur wenige,
 * seit Jahren stabile Top-Level-Schluessel angefasst (versionssicher).
 *
 * Die Direktverbindung sorgt dafuer, dass ein Bedrock-Spieler etwa so viele
 * Ressourcen braucht wie ein Java-Spieler (keine zweite interne Verbindung).
 */
public final class GeyserOptimizer {

    private final EasyBedrock plugin;

    public GeyserOptimizer(EasyBedrock plugin) {
        this.plugin = plugin;
    }

    private File geyserConfig() {
        return new File(plugin.getDataFolder().getParentFile(), "Geyser-Spigot/config.yml");
    }

    /** @return -1 wenn keine Geyser-Config existiert, sonst Anzahl Aenderungen. */
    public int optimize() {
        File config = geyserConfig();
        if (!config.exists()) {
            return -1;
        }
        try {
            List<String> lines = new ArrayList<>(
                    Files.readAllLines(config.toPath(), StandardCharsets.UTF_8));
            int changes = 0;

            if (plugin.getConfig().getBoolean("optimize.use-direct-connection", true)) {
                changes += set(lines, "use-direct-connection", "true");
            }
            if (plugin.getConfig().getBoolean("optimize.disable-compression", true)) {
                changes += set(lines, "disable-compression", "true");
            }
            if (!plugin.getConfig().getBoolean("optimize.debug-mode", false)) {
                changes += set(lines, "debug-mode", "false");
            }
            int maxPlayers = plugin.getConfig().getInt("optimize.max-players", 0);
            if (maxPlayers > 0) {
                changes += set(lines, "max-players", String.valueOf(maxPlayers));
            }

            if (changes > 0) {
                Files.write(config.toPath(), lines, StandardCharsets.UTF_8);
            }
            return changes;
        } catch (IOException e) {
            plugin.getLogger().warning("Geyser-Optimierung fehlgeschlagen: " + e.getMessage());
            return 0;
        }
    }

    /** Liest, ob die Direktverbindung aktiv ist (fuer den Status). */
    public boolean isDirectConnection() {
        File config = geyserConfig();
        if (!config.exists()) {
            return false;
        }
        try {
            for (String line : Files.readAllLines(config.toPath(), StandardCharsets.UTF_8)) {
                Matcher m = Pattern.compile("^\\s*use-direct-connection:\\s*(\\S+)").matcher(line);
                if (m.find()) {
                    return Boolean.parseBoolean(m.group(1));
                }
            }
        } catch (IOException ignored) {
            // ignorieren
        }
        return false;
    }

    /** Ersetzt den Wert eines vorhandenen Top-Level-Schluessels; 1 wenn geaendert. */
    private int set(List<String> lines, String key, String value) {
        Pattern pattern = Pattern.compile("^(" + Pattern.quote(key) + ":\\s*)(\\S.*?)(\\s*(#.*)?)$");
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = pattern.matcher(lines.get(i));
            if (m.matches()) {
                String current = m.group(2).trim();
                if (current.equalsIgnoreCase(value)) {
                    return 0;
                }
                String comment = m.group(3) == null ? "" : m.group(3);
                lines.set(i, key + ": " + value + comment);
                return 1;
            }
        }
        return 0; // Schluessel (noch) nicht vorhanden -> Geyser legt ihn beim Erststart an
    }
}
