package de.lemonpvp.smpproxy.bedrock;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeyserOptimierung {

    private static final Map<String, String> WERTE = new LinkedHashMap<>();

    static {
        WERTE.put("use-direct-connection", "true");
        WERTE.put("disable-compression", "true");
    }

    private final Path pluginOrdner;
    private final Logger log;

    public GeyserOptimierung(Path pluginOrdner, Logger log) {
        this.pluginOrdner = pluginOrdner;
        this.log = log;
    }

    public int optimieren() {
        for (String name : List.of("Geyser-Velocity", "geyser-velocity", "Geyser", "geyser")) {
            Path datei = pluginOrdner.resolve(name).resolve("config.yml");
            if (Files.isRegularFile(datei)) {
                return optimieren(datei);
            }
        }
        return -1;
    }

    public int optimieren(Path datei) {
        try {
            List<String> zeilen = new ArrayList<>(Files.readAllLines(datei, StandardCharsets.UTF_8));
            List<String> geaendert = new ArrayList<>();
            for (Map.Entry<String, String> wert : WERTE.entrySet()) {
                Pattern muster = Pattern.compile("^(\\s*)" + Pattern.quote(wert.getKey()) + ":\\s*([^#\\s]+)(\\s*#.*)?$");
                for (int i = 0; i < zeilen.size(); i++) {
                    Matcher treffer = muster.matcher(zeilen.get(i));
                    if (!treffer.matches()) {
                        continue;
                    }
                    if (!treffer.group(2).equalsIgnoreCase(wert.getValue())) {
                        String kommentar = treffer.group(3) == null ? "" : treffer.group(3);
                        zeilen.set(i, treffer.group(1) + wert.getKey() + ": " + wert.getValue() + kommentar);
                        geaendert.add(wert.getKey());
                    }
                    break;
                }
            }
            if (geaendert.isEmpty()) {
                return 0;
            }
            Path neu = datei.resolveSibling(datei.getFileName() + ".neu");
            Files.write(neu, zeilen, StandardCharsets.UTF_8);
            try {
                Files.move(neu, datei, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fehler) {
                Files.move(neu, datei, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Geyser fuer Bedrock optimiert ({}) - wirkt nach dem naechsten Proxy-Neustart.", String.join(", ", geaendert));
            return geaendert.size();
        } catch (IOException fehler) {
            log.warn("Geyser-Config konnte nicht geprueft werden: {}", fehler.getMessage());
            return 0;
        }
    }
}
