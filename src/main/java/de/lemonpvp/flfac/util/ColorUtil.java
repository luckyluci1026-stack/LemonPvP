package de.lemonpvp.flfac.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

/**
 * Helper for FLFAC's signature color gradients (Farbverläufe).
 *
 * <p>Everything is built on top of Adventure's {@link MiniMessage} so the same
 * gradient definitions work in chat, action bars, boss bars and the console.</p>
 */
public final class ColorUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtil() {
    }

    /** Parses raw MiniMessage input into a component. */
    public static Component mm(String input) {
        return MM.deserialize(input == null ? "" : input);
    }

    /** Escapes user supplied text so it cannot inject MiniMessage tags. */
    public static String escape(String text) {
        return MM.escapeTags(text == null ? "" : text);
    }

    /**
     * Builds a gradient component from a list of hex colors.
     *
     * @param text   the (already trusted) text
     * @param colors list of hex colors, e.g. {@code ["#fffb00", "#00ff00"]}
     */
    public static Component gradient(String text, List<String> colors) {
        return gradient(text, colors, 0.0D);
    }

    /**
     * Builds a gradient component with an explicit phase, useful for animation.
     *
     * @param phase value between {@code -1.0} and {@code 1.0}
     */
    public static Component gradient(String text, List<String> colors, double phase) {
        return MM.deserialize(gradientTag(colors, phase) + escape(text) + "</gradient>");
    }

    /** Builds a gradient component where the text may itself contain MiniMessage. */
    public static Component gradientRaw(String miniMessage, List<String> colors) {
        return MM.deserialize(gradientTag(colors, 0.0D) + miniMessage + "</gradient>");
    }

    /**
     * Returns a full {@code <gradient:...>content</gradient>} MiniMessage string so
     * gradients can be embedded inside a larger MiniMessage line.
     *
     * @param content content that is already MiniMessage-safe
     */
    public static String gradientWrap(String content, List<String> colors) {
        return gradientTag(colors, 0.0D) + content + "</gradient>";
    }

    private static String gradientTag(List<String> colors, double phase) {
        StringBuilder sb = new StringBuilder("<gradient");
        if (colors == null || colors.isEmpty()) {
            sb.append(":#ffffff:#aaaaaa");
        } else if (colors.size() == 1) {
            // A single color still needs two stops for a valid gradient tag.
            sb.append(':').append(colors.get(0)).append(':').append(colors.get(0));
        } else {
            for (String c : colors) {
                sb.append(':').append(c);
            }
        }
        if (phase != 0.0D) {
            sb.append(':').append(clampPhase(phase));
        }
        sb.append('>');
        return sb.toString();
    }

    private static double clampPhase(double phase) {
        if (phase > 1.0D) return 1.0D;
        if (phase < -1.0D) return -1.0D;
        return phase;
    }

    /** Serializes a component to a legacy section-coded string (for console output). */
    public static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
