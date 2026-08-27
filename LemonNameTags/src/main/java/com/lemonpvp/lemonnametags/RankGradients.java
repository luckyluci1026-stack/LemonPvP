package com.lemonpvp.lemonnametags;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the rank → name-gradient mapping from config.yml and renders a player
 * name into a MiniMessage string for the given LuckPerms group.
 *
 * <p>Keys in the {@code ranks:} section must match the player's LuckPerms primary
 * group name (lower-cased). Unknown groups fall back to {@link #DEFAULT}
 * (plain white, not bold).</p>
 */
public final class RankGradients {

    /** A single rank's name styling. {@code end == null} means a solid colour. */
    public record Gradient(String start, String end, boolean bold) {

        /** Wraps {@code name} in the MiniMessage tags for this gradient. */
        public String apply(String name) {
            String open = bold ? "<bold>" : "";
            String close = bold ? "</bold>" : "";
            if (end == null || end.isBlank() || end.equalsIgnoreCase(start)) {
                return open + "<color:" + start + ">" + name + "</color>" + close;
            }
            return open + "<gradient:" + start + ":" + end + ">" + name + "</gradient>" + close;
        }
    }

    /** Plain white, not bold — used for any unmapped group. */
    public static final Gradient DEFAULT = new Gradient("#ffffff", null, false);

    private final Map<String, Gradient> byGroup = new HashMap<>();

    /** Reloads the mapping from the given config. */
    public void load(ConfigurationSection root, boolean defaultBold) {
        byGroup.clear();
        if (root == null) return;
        ConfigurationSection ranks = root.getConfigurationSection("ranks");
        if (ranks == null) return;

        for (String key : ranks.getKeys(false)) {
            ConfigurationSection r = ranks.getConfigurationSection(key);
            if (r == null) continue;
            String start = r.getString("start");
            if (start == null || start.isBlank()) continue;
            String end = r.getString("end"); // null = solid
            boolean bold = r.getBoolean("bold", defaultBold);
            byGroup.put(key.toLowerCase(Locale.ROOT), new Gradient(start, end, bold));
        }
    }

    /** The gradient for a LuckPerms group, or {@link #DEFAULT} if unmapped. */
    public Gradient forGroup(String group) {
        if (group == null) return DEFAULT;
        return byGroup.getOrDefault(group.toLowerCase(Locale.ROOT), DEFAULT);
    }

    public int size() {
        return byGroup.size();
    }
}
