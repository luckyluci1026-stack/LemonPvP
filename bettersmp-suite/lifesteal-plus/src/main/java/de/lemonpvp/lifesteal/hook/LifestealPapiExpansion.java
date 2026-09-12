package de.lemonpvp.lifesteal.hook;

import de.lemonpvp.lifesteal.LifestealPlus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI-Expansion: %lifesteal_hearts%, %lifesteal_maxhearts%,
 * %lifesteal_health%, %lifesteal_eliminated%, %lifesteal_status%.
 * Wird nur registriert, wenn PlaceholderAPI vorhanden ist.
 */
public final class LifestealPapiExpansion extends PlaceholderExpansion {

    private final LifestealPlus plugin;

    public LifestealPapiExpansion(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lifesteal";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        int hearts = plugin.hearts().getHearts(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "hearts" -> String.valueOf(hearts);
            case "maxhearts" -> String.valueOf(plugin.hearts().maximum());
            case "health" -> String.valueOf(hearts * 2);
            case "eliminated" -> plugin.hearts().isEliminated(player.getUniqueId()) ? "true" : "false";
            case "status" -> plugin.hearts().isEliminated(player.getUniqueId())
                    ? "Eliminiert" : "Aktiv";
            default -> null;
        };
    }
}
