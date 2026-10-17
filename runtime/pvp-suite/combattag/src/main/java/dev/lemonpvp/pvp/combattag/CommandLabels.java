package dev.lemonpvp.pvp.combattag;

import java.util.Locale;

/** Extracts the command name from what a player typed. */
final class CommandLabels {

    private CommandLabels() {
    }

    /**
     * {@code "/spawn"} → {@code "spawn"}, {@code "/Essentials:Home base"} → {@code "home"}.
     *
     * @param message the chat line starting with a slash
     * @return the lower-case command name without namespace, or {@code ""} if there is none
     */
    static String of(String message) {
        String line = message.startsWith("/") ? message.substring(1) : message;
        String label = line.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.lastIndexOf(':');
        return colon >= 0 ? label.substring(colon + 1) : label;
    }
}
