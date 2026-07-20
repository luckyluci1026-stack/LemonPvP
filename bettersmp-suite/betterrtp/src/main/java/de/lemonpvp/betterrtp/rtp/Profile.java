package de.lemonpvp.betterrtp.rtp;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Welt-Profil fuer RTP: Suchbereich, Weltborder-Beachtung, Biom-Blacklist.
 */
public record Profile(
        boolean enabled,
        int centerX,
        int centerZ,
        int minRadius,
        int maxRadius,
        boolean respectWorldborder,
        Set<String> blacklistBiomes
) {

    public static Profile from(ConfigurationSection sec) {
        Set<String> biomes = new HashSet<>();
        for (String biome : sec.getStringList("blacklist-biomes")) {
            biomes.add(biome.toLowerCase(Locale.ROOT).trim());
        }
        int min = Math.max(0, sec.getInt("min-radius", 500));
        int max = Math.max(min + 1, sec.getInt("max-radius", 10000));
        return new Profile(
                sec.getBoolean("enabled", true),
                sec.getInt("center-x", 0),
                sec.getInt("center-z", 0),
                min,
                max,
                sec.getBoolean("respect-worldborder", true),
                biomes
        );
    }
}
