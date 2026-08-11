package de.lemonpvp.smpproxy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Kleiner Helfer für die Nachrichten aus der config.yml.
 * Alles läuft über MiniMessage, damit Gradients und Farben funktionieren.
 */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Msg() {
    }

    /**
     * Wandelt eine Zeile aus der config.yml in eine Chat-Nachricht um.
     * Platzhalter werden paarweise übergeben: ("%server%", "smp1", ...)
     */
    public static Component of(String raw, String prefix, String... placeholders) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String text = raw.replace("%prefix%", prefix == null ? "" : prefix);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return MM.deserialize(text);
    }

    public static Component plain(String raw) {
        return raw == null || raw.isEmpty() ? Component.empty() : MM.deserialize(raw);
    }
}
