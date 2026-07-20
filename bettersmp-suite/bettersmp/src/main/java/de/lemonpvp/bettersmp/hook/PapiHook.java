package de.lemonpvp.bettersmp.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Wendet PlaceholderAPI-Platzhalter an, falls PlaceholderAPI installiert ist.
 */
public final class PapiHook {

    private final boolean available;

    public PapiHook() {
        this.available = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isAvailable() {
        return available;
    }

    public String apply(Player player, String text) {
        if (!available || text == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            return Bridge.apply(player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    private static final class Bridge {
        static String apply(Player player, String text) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}
