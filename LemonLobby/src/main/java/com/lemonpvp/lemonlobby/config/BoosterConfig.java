package com.lemonpvp.lemonlobby.config;

import com.lemonpvp.lemonlobby.model.BoosterTier;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

/**
 * Loads booster tier definitions from boosters.lemon.
 * Call reload() on startup and after /llobby reload.
 */
public class BoosterConfig {

    private final Plugin plugin;
    private final List<BoosterTier> tiers = new ArrayList<>();

    public BoosterConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        tiers.clear();

        File file = new File(plugin.getDataFolder(), "boosters.lemon");
        if (!file.exists()) {
            plugin.saveResource("boosters.lemon", false);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentId       = null;
            String currentName     = null;
            long   currentPrice    = 0;
            int    currentBonus    = 0;
            int    currentDuration = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("booster ")) {
                    if (currentId != null) {
                        int level = tiers.size() + 1;
                        tiers.add(new BoosterTier(currentId, level, currentName,
                                currentPrice, currentBonus, currentDuration));
                    }
                    currentId       = line.substring(8).trim();
                    currentName     = currentId;
                    currentPrice    = 0;
                    currentBonus    = 0;
                    currentDuration = 0;
                } else if (currentId != null) {
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length < 2) continue;
                    String key   = parts[0];
                    String value = parts[1].replace("\"", "").replace("_", "").trim();
                    switch (key) {
                        case "name"  -> currentName     = parts[1].replace("\"", "").trim();
                        case "preis" -> currentPrice    = Long.parseLong(value);
                        case "bonus" -> currentBonus    = Integer.parseInt(value);
                        case "dauer" -> currentDuration = Integer.parseInt(value);
                    }
                }
            }

            // flush last block
            if (currentId != null) {
                int level = tiers.size() + 1;
                tiers.add(new BoosterTier(currentId, level, currentName,
                        currentPrice, currentBonus, currentDuration));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load boosters.lemon: " + e.getMessage());
        }

        plugin.getLogger().info("Loaded " + tiers.size() + " booster tiers from boosters.lemon.");
    }

    public List<BoosterTier> getTiers() {
        return Collections.unmodifiableList(tiers);
    }

    /** Returns the tier with the given level (1-based), or null. */
    public BoosterTier fromLevel(int level) {
        for (BoosterTier t : tiers) {
            if (t.level == level) return t;
        }
        return null;
    }

    /** Returns the tier with the given id (e.g. "L1"), or null. */
    public BoosterTier fromId(String id) {
        for (BoosterTier t : tiers) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }
}
