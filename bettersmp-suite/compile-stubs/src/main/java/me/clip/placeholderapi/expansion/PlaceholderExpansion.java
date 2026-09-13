package me.clip.placeholderapi.expansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Compile-only stub of PlaceholderAPI's PlaceholderExpansion.
 * At runtime the real abstract class from the installed PlaceholderAPI
 * plugin is used; this stub is never packaged (scope: provided).
 */
public abstract class PlaceholderExpansion {

    public abstract String getIdentifier();

    public abstract String getAuthor();

    public abstract String getVersion();

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        return null;
    }

    public String onPlaceholderRequest(Player player, String params) {
        return null;
    }

    public final boolean register() {
        throw new IllegalStateException("compile stub - the real PlaceholderAPI class is used at runtime");
    }
}
