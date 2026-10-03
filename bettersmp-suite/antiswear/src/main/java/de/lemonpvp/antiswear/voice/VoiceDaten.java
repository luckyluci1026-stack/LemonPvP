package de.lemonpvp.antiswear.voice;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceDaten {

    public static final byte STUMM = 1;
    public static final byte REGELN = 2;
    public static final long FUER_IMMER = Long.MAX_VALUE;

    public record Eintrag(long wert, long zeit, String name, String von, String grund) {

        public Eintrag {
            name = name == null ? "" : name;
            von = von == null ? "" : von;
            grund = grund == null ? "" : grund;
        }
    }

    private final Map<UUID, Eintrag> stumm = new ConcurrentHashMap<>();
    private final Map<UUID, Eintrag> regeln = new ConcurrentHashMap<>();

    public static boolean bekannteArt(byte art) {
        return art == STUMM || art == REGELN;
    }

    private Map<UUID, Eintrag> liste(byte art) {
        if (art == STUMM) {
            return stumm;
        }
        if (art == REGELN) {
            return regeln;
        }
        throw new IllegalArgumentException("Unbekannte Art " + art);
    }

    public Eintrag eintrag(byte art, UUID spieler) {
        return liste(art).get(spieler);
    }

    public boolean uebernehmen(byte art, UUID spieler, Eintrag neu) {
        boolean[] geaendert = {false};
        liste(art).compute(spieler, (id, alt) -> {
            if (alt != null && alt.zeit() >= neu.zeit()) {
                return alt;
            }
            geaendert[0] = true;
            return neu;
        });
        return geaendert[0];
    }

    public long naechsteZeit(byte art, UUID spieler, long jetzt) {
        Eintrag alt = eintrag(art, spieler);
        return alt == null ? jetzt : Math.max(jetzt, alt.zeit() + 1);
    }

    public long stummBis(UUID spieler, long jetzt) {
        Eintrag eintrag = stumm.get(spieler);
        return eintrag == null || eintrag.wert() <= jetzt ? 0 : eintrag.wert();
    }

    public int zugestimmteVersion(UUID spieler) {
        Eintrag eintrag = regeln.get(spieler);
        return eintrag == null ? 0 : (int) Math.min(Integer.MAX_VALUE, eintrag.wert());
    }

    public Map<UUID, Eintrag> aktiveStummschaltungen(long jetzt) {
        Map<UUID, Eintrag> aktiv = new LinkedHashMap<>();
        stumm.entrySet().stream()
                .filter(e -> e.getValue().wert() > jetzt)
                .sorted(Map.Entry.comparingByValue((a, b) -> Long.compare(a.wert(), b.wert())))
                .forEach(e -> aktiv.put(e.getKey(), e.getValue()));
        return aktiv;
    }

    public void laden(YamlConfiguration yaml, long jetzt, long aufbewahrenMillis) {
        stumm.clear();
        regeln.clear();
        lesen(yaml.getConfigurationSection("stumm"), stumm);
        lesen(yaml.getConfigurationSection("regeln"), regeln);
        stumm.entrySet().removeIf(e -> e.getValue().wert() <= jetzt && e.getValue().zeit() < jetzt - aufbewahrenMillis);
    }

    private static void lesen(ConfigurationSection bereich, Map<UUID, Eintrag> ziel) {
        if (bereich == null) {
            return;
        }
        for (String schluessel : bereich.getKeys(false)) {
            ConfigurationSection eintrag = bereich.getConfigurationSection(schluessel);
            if (eintrag == null) {
                continue;
            }
            try {
                ziel.put(UUID.fromString(schluessel), new Eintrag(eintrag.getLong("wert"), eintrag.getLong("zeit"),
                        eintrag.getString("name", ""), eintrag.getString("von", ""), eintrag.getString("grund", "")));
            } catch (IllegalArgumentException ignoriert) {
            }
        }
    }

    public YamlConfiguration alsYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        schreiben(yaml, "stumm", stumm);
        schreiben(yaml, "regeln", regeln);
        return yaml;
    }

    private static void schreiben(YamlConfiguration yaml, String pfad, Map<UUID, Eintrag> quelle) {
        yaml.createSection(pfad);
        quelle.forEach((id, eintrag) -> {
            String basis = pfad + "." + id;
            yaml.set(basis + ".wert", eintrag.wert());
            yaml.set(basis + ".zeit", eintrag.zeit());
            yaml.set(basis + ".name", eintrag.name());
            if (!eintrag.von().isEmpty()) {
                yaml.set(basis + ".von", eintrag.von());
            }
            if (!eintrag.grund().isEmpty()) {
                yaml.set(basis + ".grund", eintrag.grund());
            }
        });
    }
}
