package de.lemonpvp.helden.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Farb- und Platzhalter-Helfer.
 *
 * <p>Bedrock-Clients koennen keine Hex-Farben darstellen. {@link #colorLegacy(String)}
 * rechnet {@code &#RRGGBB} deshalb auf die naechstliegende der 16 klassischen
 * Farben herunter, damit Bedrock-Spieler keine kaputten Codes sehen.</p>
 */
public final class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");

    private static final Map<ChatColor, int[]> LEGACY_RGB = new LinkedHashMap<>();

    static {
        LEGACY_RGB.put(ChatColor.BLACK, new int[]{0x00, 0x00, 0x00});
        LEGACY_RGB.put(ChatColor.DARK_BLUE, new int[]{0x00, 0x00, 0xAA});
        LEGACY_RGB.put(ChatColor.DARK_GREEN, new int[]{0x00, 0xAA, 0x00});
        LEGACY_RGB.put(ChatColor.DARK_AQUA, new int[]{0x00, 0xAA, 0xAA});
        LEGACY_RGB.put(ChatColor.DARK_RED, new int[]{0xAA, 0x00, 0x00});
        LEGACY_RGB.put(ChatColor.DARK_PURPLE, new int[]{0xAA, 0x00, 0xAA});
        LEGACY_RGB.put(ChatColor.GOLD, new int[]{0xFF, 0xAA, 0x00});
        LEGACY_RGB.put(ChatColor.GRAY, new int[]{0xAA, 0xAA, 0xAA});
        LEGACY_RGB.put(ChatColor.DARK_GRAY, new int[]{0x55, 0x55, 0x55});
        LEGACY_RGB.put(ChatColor.BLUE, new int[]{0x55, 0x55, 0xFF});
        LEGACY_RGB.put(ChatColor.GREEN, new int[]{0x55, 0xFF, 0x55});
        LEGACY_RGB.put(ChatColor.AQUA, new int[]{0x55, 0xFF, 0xFF});
        LEGACY_RGB.put(ChatColor.RED, new int[]{0xFF, 0x55, 0x55});
        LEGACY_RGB.put(ChatColor.LIGHT_PURPLE, new int[]{0xFF, 0x55, 0xFF});
        LEGACY_RGB.put(ChatColor.YELLOW, new int[]{0xFF, 0xFF, 0x55});
        LEGACY_RGB.put(ChatColor.WHITE, new int[]{0xFF, 0xFF, 0xFF});
    }

    private Text() {
    }

    /** Uebersetzt {@code &}-Codes und {@code &#RRGGBB} in echte Farben. */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(toHexSequence(matcher.group(1))));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    /**
     * Wie {@link #color(String)}, reduziert Hex-Farben aber auf die naechste
     * Legacy-Farbe. Genau das brauchen Bedrock-Clients.
     */
    public static String colorLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(nearestLegacy(matcher.group(1)).toString()));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<>();
        if (input == null) {
            return out;
        }
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }

    /**
     * Ersetzt Platzhalter. Die Argumente werden paarweise gelesen:
     * {@code replace(msg, "%hero%", "Krieger", "%lives%", 3)}.
     */
    public static String replace(String input, Object... placeholders) {
        if (input == null) {
            return "";
        }
        String out = input;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = placeholders[i + 1] == null ? "" : String.valueOf(placeholders[i + 1]);
            out = out.replace(key, value);
        }
        return out;
    }

    public static String strip(String input) {
        return input == null ? "" : ChatColor.stripColor(color(input));
    }

    /** Schneidet einen Text hart ab - Scoreboard-Prefixe moegen keine Romane. */
    public static String truncate(String input, int max) {
        if (input == null) {
            return "";
        }
        return input.length() <= max ? input : input.substring(0, max);
    }

    /** Erste Buchstabe gross, Rest klein - fuer IDs aus der Config. */
    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }

    private static String toHexSequence(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            builder.append('§').append(c);
        }
        return builder.toString();
    }

    private static ChatColor nearestLegacy(String hex) {
        int rgb = Integer.parseInt(hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        ChatColor best = ChatColor.WHITE;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<ChatColor, int[]> entry : LEGACY_RGB.entrySet()) {
            int[] c = entry.getValue();
            // Gewichteter RGB-Abstand, kommt der menschlichen Wahrnehmung naeher.
            double distance = 2.0 * Math.pow(r - c[0], 2)
                    + 4.0 * Math.pow(g - c[1], 2)
                    + 3.0 * Math.pow(b - c[2], 2);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }
        return best;
    }
}
