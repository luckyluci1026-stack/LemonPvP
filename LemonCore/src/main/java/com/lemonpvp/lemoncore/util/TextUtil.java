package com.lemonpvp.lemoncore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextUtil {

    /** Maps a-z onto Unicode small-cap glyphs (no resource pack needed). */
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ";

    /**
     * Custom {@code <smallcaps>} tag: renders the wrapped text with Unicode
     * small-cap glyphs, so "<smallcaps>Queue</smallcaps>" shows as "Qᴜᴇᴜᴇ"
     * without hand-typing the special characters. Works in vanilla clients.
     */
    private static final TagResolver SMALLCAPS_TAG = TagResolver.resolver("smallcaps",
            (args, ctx) -> (net.kyori.adventure.text.minimessage.tag.Modifying)
                    (current, depth) -> {
                        if (current instanceof net.kyori.adventure.text.TextComponent tc) {
                            return tc.content(smallCaps(tc.content()));
                        }
                        return current;
                    });

    private static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolver(SMALLCAPS_TAG))
            .build();

    /** Converts lowercase letters to Unicode small-caps (uppercase kept as-is). */
    public static String smallCaps(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            char lower = Character.toLowerCase(c);
            sb.append(lower >= 'a' && lower <= 'z' ? SMALL_CAPS.charAt(lower - 'a') : c);
        }
        return sb.toString();
    }

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return MM.deserialize(text);
    }

    public static Component parse(String text, Map<String, String> placeholders) {
        if (text == null) return Component.empty();
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return MM.deserialize(text, TagResolver.resolver(resolvers));
    }

    public static String stripTags(String text) {
        if (text == null) return "";
        Component parsed = MM.deserialize(text);
        return PlainTextComponentSerializer.plainText().serialize(parsed);
    }

    public static String escapeTags(String text) {
        if (text == null) return "";
        return MM.escapeTags(text);
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "Permanent";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins > 0) sb.append(mins).append("m ");
        if (secs > 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }

    /** Returned by {@link #parseDuration(String)} when the input is not a valid duration. */
    public static final long INVALID_DURATION = -1L;

    /**
     * Parses a duration such as {@code 7d}, {@code 12h} or {@code 1d 12h} into seconds.
     *
     * <p>Returns {@code 0} <em>only</em> for an explicit {@code "0"} or {@code "permanent"} —
     * the sentinel the rest of the plugin reads as "never expires". Anything the grammar does
     * not accept ({@code "30"} with no unit, an unknown unit like {@code "30x"}, stray text,
     * arithmetic overflow) returns {@link #INVALID_DURATION} rather than {@code 0}: silently
     * treating a typo as "permanent" would turn a temporary punishment into a permanent one.
     * Callers with a sensible default should use {@link #parseDuration(String, long)}.
     */
    public static long parseDuration(String input) {
        if (input == null) return INVALID_DURATION;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return INVALID_DURATION;
        if (trimmed.equals("0") || trimmed.equalsIgnoreCase("permanent")) return 0;

        long total = 0;
        boolean sawUnit = false;
        StringBuilder num = new StringBuilder();
        for (char c : trimmed.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
                continue;
            }
            if (num.length() == 0) {
                // Tolerate the spacing in "1d 12h"; reject a unit with no number before it.
                if (Character.isWhitespace(c)) continue;
                return INVALID_DURATION;
            }
            long unitSeconds = switch (c) {
                case 's' -> 1L;
                case 'm' -> 60L;
                case 'h' -> 3600L;
                case 'd' -> 86400L;
                case 'w' -> 604800L;
                case 'y' -> 31536000L;
                default  -> INVALID_DURATION;
            };
            if (unitSeconds == INVALID_DURATION) return INVALID_DURATION;
            try {
                total = Math.addExact(total,
                        Math.multiplyExact(Long.parseLong(num.toString()), unitSeconds));
            } catch (ArithmeticException | NumberFormatException e) {
                return INVALID_DURATION; // overflow, or a digit run too long for a long
            }
            num = new StringBuilder();
            sawUnit = true;
        }
        // Trailing digits with no unit ("30"): reject instead of guessing seconds or days.
        if (num.length() > 0) return INVALID_DURATION;
        return sawUnit ? total : INVALID_DURATION;
    }

    /**
     * {@link #parseDuration(String)} with a fallback: yields {@code fallbackSeconds} when the
     * input cannot be parsed, so a malformed value degrades to a sane duration rather than to
     * "permanent".
     */
    public static long parseDuration(String input, long fallbackSeconds) {
        long parsed = parseDuration(input);
        return parsed == INVALID_DURATION ? fallbackSeconds : parsed;
    }

    public static String generateId(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Formats a coin amount into a compact human-readable string.
     * 1 500 → 1.5k | 1 000 000 → 1M | 1 000 000 000 → 1B |
     * 1 000 000 000 000 → 1T | ≥ 1 quadrillion → 999T+
     */
    public static String formatCoins(long amount) {
        if (amount < 0) return "-" + formatCoins(-amount);
        if (amount >= 1_000_000_000_000_000L) return "999T+";

        long[] thresholds = {
            1_000_000_000_000L,   // T
            1_000_000_000L,       // B
            1_000_000L,           // M
            1_000L                // k
        };
        String[] suffixes = {"T", "B", "M", "k"};

        for (int i = 0; i < thresholds.length; i++) {
            if (amount >= thresholds[i]) {
                long whole = amount / thresholds[i];
                long dec   = (amount % thresholds[i]) / (thresholds[i] / 10);
                return dec > 0 ? whole + "." + dec + suffixes[i] : whole + suffixes[i];
            }
        }
        return String.valueOf(amount);
    }

    public static String generateAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
