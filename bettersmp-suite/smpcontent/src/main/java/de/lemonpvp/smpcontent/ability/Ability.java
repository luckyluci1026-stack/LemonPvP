package de.lemonpvp.smpcontent.ability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Eine Spezialfähigkeit: ein Auslöser und die Dinge, die dann passieren.
 *
 * <pre>
 * abilities:
 *   - trigger: right-click
 *     name: "Laserstrahl"
 *     cooldown: 6
 *     actions:
 *       - type: animation
 *         shape: beam
 * </pre>
 */
public record Ability(String trigger, String name, int cooldownSeconds,
                      String permission, List<Map<String, Object>> actions) {

    /** Baut eine Fähigkeit aus dem, was in der YAML-Datei steht. */
    public static Ability from(Map<String, Object> raw) {
        String trigger = string(raw, "trigger", "right-click").toLowerCase(Locale.ROOT);
        List<Map<String, Object>> actions = new ArrayList<>();
        Object list = raw.get("actions");
        if (list instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> map) {
                    actions.add(normalise(map));
                }
            }
        }
        return new Ability(trigger,
                string(raw, "name", ""),
                (int) number(raw, "cooldown", 0),
                string(raw, "permission", ""),
                actions);
    }

    // ------------------------------------------------------------------
    //  Kleine Helfer - die Werte kommen als rohe YAML-Maps herein
    // ------------------------------------------------------------------

    /** Macht aus einer beliebigen YAML-Map eine Map mit String-Schlüsseln. */
    public static Map<String, Object> normalise(Map<?, ?> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    public static String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // Standardwert benutzen
            }
        }
        return fallback;
    }

    public static boolean flag(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value).trim());
    }
}
