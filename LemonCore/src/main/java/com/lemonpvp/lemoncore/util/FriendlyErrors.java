package com.lemonpvp.lemoncore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;

/**
 * Translates raw Java/Netty/Paper disconnect reasons into messages a normal
 * player can act on. Technical detail stays visible in a small gray line so
 * staff can still diagnose; unknown reasons pass through untouched (ban and
 * kick screens are already player-friendly and never look like stack traces).
 */
public final class FriendlyErrors {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private FriendlyErrors() {}

    /** True if the reason looks like a technical error rather than a human message. */
    public static boolean isTechnical(String plain) {
        String s = plain.toLowerCase(Locale.ROOT);
        return s.contains("exception") || s.contains("timed out") || s.contains("timeout")
                || s.contains("connection reset") || s.contains("closed by")
                || s.contains("io.netty") || s.contains("java.") || s.contains("decoder")
                || s.contains("badly compressed") || s.contains("packet")
                || s.contains("invalid player data") || s.contains("out of memory")
                || s.contains("flying is not enabled");
    }

    /** Friendly component for a raw reason, or the original if it isn't technical. */
    public static Component translate(Component raw) {
        String plain = PlainTextComponentSerializer.plainText().serialize(raw);
        if (!isTechnical(plain)) return raw;
        return build(headline(plain), plain);
    }

    private static String headline(String plain) {
        String s = plain.toLowerCase(Locale.ROOT);
        if (s.contains("timed out") || s.contains("timeout")) {
            return "<yellow>Your connection timed out.</yellow>\n<gray>Check your internet and rejoin.";
        }
        if (s.contains("connection reset") || s.contains("closed by")) {
            return "<yellow>The connection was interrupted.</yellow>\n<gray>Just rejoin — nothing is lost.";
        }
        if (s.contains("out of memory")) {
            return "<yellow>The server had a hiccup.</yellow>\n<gray>Please rejoin in a moment.";
        }
        if (s.contains("flying is not enabled")) {
            return "<yellow>The server thought you were flying.</yellow>\n<gray>That can happen with lag — just rejoin.";
        }
        if (s.contains("badly compressed") || s.contains("packet") || s.contains("decoder")) {
            return "<yellow>Something went wrong with your connection data.</yellow>\n<gray>Rejoining usually fixes this.";
        }
        return "<yellow>An unexpected error occurred.</yellow>\n<gray>Please rejoin — if it keeps happening, report it with /bugreport.";
    }

    private static Component build(String headlineMini, String technicalPlain) {
        String tech = technicalPlain.length() > 120 ? technicalPlain.substring(0, 120) + "…" : technicalPlain;
        return MM.deserialize(
                "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient>\n\n"
                + headlineMini
                + "\n\n<dark_gray>Details: " + tech.replace("<", "\\<"));
    }
}
