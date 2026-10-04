package de.lemonpvp.smpcontent.boss;

import de.lemonpvp.smpcontent.ability.Ability;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ein Boss, wie er in der bosse.yml steht.
 *
 * Ein Boss ist ein ganz normales Mob mit aufgebohrten Werten, einer
 * Lebensleiste am oberen Bildschirmrand und - das ist der eigentliche Punkt -
 * einer Liste von Angriffen, die sich mit sinkendem Leben ändert. Jede
 * Phase hat ihre eigenen Angriffe; wer den Boss auf die Hälfte bringt,
 * bekommt es mit etwas anderem zu tun als am Anfang.
 *
 * Alles hier steht in der Datei. Im Plugin ist nichts über einen bestimmten
 * Boss festgeschrieben - wer einen eigenen will, schreibt ihn hin.
 */
public record BossType(String id, String mob, String name, double health, double armor,
                       double speed, double knockbackResist, double scale, boolean glow,
                       boolean feuerfest, Map<String, String> gear,
                       BarColor barColor, BarStyle barStyle,
                       List<Phase> phases, List<Drop> drops, String deathMessage,
                       String spawnMessage) {

    /**
     * Ein Abschnitt des Kampfes.
     *
     * "ab" ist der Lebensanteil in Prozent, ab dem die Phase gilt. Gesucht
     * wird von oben nach unten die erste, deren Schwelle noch erreicht ist.
     */
    public record Phase(String name, double ab, List<Attack> attacks, String enterAnimation,
                        String enterParticle, String enterColor, String enterSound) { }

    /**
     * Ein Angriff.
     *
     * "typ" sagt, was passiert, der Rest sind Zahlen dazu. Die Animation ist
     * eine Form aus der animationen.yml - dadurch sieht jeder Angriff so aus,
     * wie du es dort einstellst, ohne dass im Plugin etwas geändert wird.
     */
    public record Attack(String type, double cooldown, double range, double damage,
                         double value, int count, String animation, String particle,
                         String color, String sound, String mob, String potion,
                         int potionSeconds, int potionLevel, String message) {

        static Attack from(ConfigurationSection s) {
            return new Attack(
                    s.getString("typ", s.getString("type", "welle")).toLowerCase(Locale.ROOT),
                    s.getDouble("abklingzeit", s.getDouble("cooldown", 5.0)),
                    s.getDouble("reichweite", s.getDouble("range", 8.0)),
                    s.getDouble("schaden", s.getDouble("damage", 6.0)),
                    s.getDouble("staerke", s.getDouble("value", 1.0)),
                    s.getInt("anzahl", s.getInt("count", 1)),
                    s.getString("animation", ""),
                    s.getString("teilchen", s.getString("particle", "DUST")),
                    s.getString("farbe", s.getString("color", "#FFFFFF")),
                    s.getString("ton", s.getString("sound", "")),
                    s.getString("mob", "ZOMBIE"),
                    s.getString("trank", s.getString("potion", "")),
                    s.getInt("trank-sekunden", 6),
                    s.getInt("trank-stufe", 1),
                    s.getString("ansage", s.getString("message", "")));
        }
    }

    /** Was beim Tod herausfällt. */
    public record Drop(String item, int min, int max, double chance) { }

    public static BossType read(String id, ConfigurationSection s) {
        Map<String, String> gear = new LinkedHashMap<>();
        ConfigurationSection aus = s.getConfigurationSection("ausruestung");
        if (aus == null) {
            aus = s.getConfigurationSection("gear");
        }
        if (aus != null) {
            for (String slot : aus.getKeys(false)) {
                gear.put(slot.toLowerCase(Locale.ROOT), aus.getString(slot, ""));
            }
        }

        List<Phase> phases = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("phasen").isEmpty()
                ? s.getMapList("phases") : s.getMapList("phasen")) {
            phases.add(phase(raw));
        }
        if (phases.isEmpty()) {
            phases.add(new Phase("", 100, List.of(), "", "DUST", "#FFFFFF", ""));
        }
        // Die höchste Schwelle zuerst - so findet die Suche die richtige Phase
        phases.sort((a, b) -> Double.compare(b.ab(), a.ab()));

        List<Drop> drops = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("beute").isEmpty()
                ? s.getMapList("drops") : s.getMapList("beute")) {
            Map<String, Object> m = Ability.normalise(raw);
            drops.add(new Drop(Ability.string(m, "item", ""),
                    (int) Ability.number(m, "min", 1),
                    (int) Ability.number(m, "max", 1),
                    Ability.number(m, "chance", 1.0)));
        }

        return new BossType(id,
                s.getString("typ", s.getString("mob", "ZOMBIE")).toUpperCase(Locale.ROOT),
                s.getString("name", id),
                Math.max(1, s.getDouble("leben", s.getDouble("health", 200))),
                s.getDouble("ruestung", s.getDouble("armor", 0)),
                s.getDouble("tempo", s.getDouble("speed", 0.25)),
                s.getDouble("standfest", s.getDouble("knockback-resist", 0.6)),
                s.getDouble("groesse", s.getDouble("scale", 1.0)),
                s.getBoolean("leuchten", true),
                s.getBoolean("feuerfest", true),
                gear,
                barColor(s.getString("bossbar-farbe", s.getString("bar-color", "RED"))),
                barStyle(s.getString("bossbar-stil", s.getString("bar-style", "SEGMENTED_10"))),
                Collections.unmodifiableList(phases),
                Collections.unmodifiableList(drops),
                s.getString("todesansage", ""),
                s.getString("ansage", ""));
    }

    private static Phase phase(Map<?, ?> raw) {
        Map<String, Object> m = Ability.normalise(raw);
        List<Attack> attacks = new ArrayList<>();
        Object list = m.containsKey("angriffe") ? m.get("angriffe") : m.get("attacks");
        if (list instanceof List<?> items) {
            for (Object one : items) {
                if (one instanceof Map<?, ?> map) {
                    attacks.add(Attack.from(section(Ability.normalise(map))));
                }
            }
        }
        return new Phase(Ability.string(m, "name", ""),
                Ability.number(m, "ab", Ability.number(m, "at", 100)),
                Collections.unmodifiableList(attacks),
                Ability.string(m, "eintritt-animation", ""),
                Ability.string(m, "eintritt-teilchen", "DUST"),
                Ability.string(m, "eintritt-farbe", "#FFFFFF"),
                Ability.string(m, "eintritt-ton", ""));
    }

    /** Eine Map als ConfigurationSection, damit Attack.from() sie lesen kann. */
    private static ConfigurationSection section(Map<String, Object> map) {
        org.bukkit.configuration.MemoryConfiguration cfg =
                new org.bukkit.configuration.MemoryConfiguration();
        map.forEach(cfg::set);
        return cfg;
    }

    private static BarColor barColor(String value) {
        try {
            return BarColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BarColor.RED;
        }
    }

    private static BarStyle barStyle(String value) {
        try {
            return BarStyle.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BarStyle.SEGMENTED_10;
        }
    }

    /**
     * Die Phase, die bei diesem Lebensanteil (0..1) gilt.
     *
     * Bei den Schwellen 100, 60 und 25 heißt das: von 100 bis 60 die erste,
     * von 60 bis 25 die zweite, darunter die dritte. Gesucht ist also nicht
     * die erste passende, sondern die <em>tiefste</em>, die noch passt - die
     * Liste ist absteigend sortiert, deshalb geht es weiter, solange die
     * Schwelle noch über dem Leben liegt.
     */
    public Phase phaseFor(double anteil) {
        double prozent = anteil * 100;
        Phase treffer = phases.get(0);
        for (Phase phase : phases) {
            if (prozent > phase.ab()) {
                break;
            }
            treffer = phase;
        }
        return treffer;
    }
}
