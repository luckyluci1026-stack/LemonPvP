package com.lemonpvp.lemonqueue.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

/**
 * Gradient helpers built on MiniMessage (bundled with Velocity).
 *
 * <p>The {@code *Animated} variants shift the gradient phase based on the
 * current wall-clock time, producing a smooth flowing effect when sent
 * repeatedly (e.g. via the action bar every few hundred ms).</p>
 */
public final class Gradients {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Gradients() {}

    // ── Static gradients ─────────────────────────────────────────────────

    /** Lemon branding: bright yellow → spring green. */
    public static Component lemon(String text) {
        return parse("<gradient:#fff700:#00ff7f>" + text + "</gradient>");
    }

    /** Warning / "server full": gold → orange → red. */
    public static Component fire(String text) {
        return parse("<gradient:#ffd000:#ff5e00:#ff0040>" + text + "</gradient>");
    }

    /** Cool accent: aqua → blue → violet. */
    public static Component cool(String text) {
        return parse("<gradient:#00e5ff:#2979ff:#7c4dff>" + text + "</gradient>");
    }

    // ── Animated gradients ───────────────────────────────────────────────

    /** Flowing lemon gradient (≈2.5 s loop). */
    public static Component lemonAnimated(String text) {
        return parse("<gradient:#fff700:#aaff00:#00ff7f:" + fmt(phase(2500)) + ">" + text + "</gradient>");
    }

    /** Flowing fire gradient (≈2 s loop) for the "full" / waiting state. */
    public static Component fireAnimated(String text) {
        return parse("<gradient:#ffd000:#ff8a00:#ff2d55:" + fmt(phase(2000)) + ">" + text + "</gradient>");
    }

    /** Flowing rainbow gradient (≈4 s loop) for celebratory messages. */
    public static Component rainbowAnimated(String text) {
        return parse("<gradient:#ff0040:#ff8a00:#fff700:#00ff7f:#00e5ff:#7c4dff:" + fmt(phase(4000)) + ">"
                + text + "</gradient>");
    }

    /** Flowing cool gradient (≈3 s loop): aqua → blue → violet. */
    public static Component coolAnimated(String text) {
        return parse("<gradient:#00e5ff:#2979ff:#7c4dff:" + fmt(phase(3000)) + ">" + text + "</gradient>");
    }

    /**
     * Parses an arbitrary MiniMessage template after substituting the supplied
     * {@code {key}} placeholders. Used for user-configurable title/subtitle
     * lines so server owners can supply their own gradients and tags.
     */
    public static Component template(String template, Map<String, String> placeholders) {
        String out = template;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return parse(out);
    }

    // ── Internals ────────────────────────────────────────────────────────

    /** Returns a phase in the range [-1, 1] cycling once per {@code periodMs}. */
    private static double phase(long periodMs) {
        double t = (System.currentTimeMillis() % periodMs) / (double) periodMs; // 0..1
        return t * 2.0 - 1.0;
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.US, "%.3f", d);
    }

    /** Parses MiniMessage, falling back to plain text on any markup error. */
    private static Component parse(String mini) {
        try {
            return MM.deserialize(mini);
        } catch (Exception e) {
            return Component.text(mini.replaceAll("<[^>]*>", ""));
        }
    }
}
