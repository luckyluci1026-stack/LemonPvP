package de.lemonpvp.helden.storage;

import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Speichert alle Profile in einer einzigen data/players.yml. */
public final class YamlStorage implements Storage {

    private final Plugin plugin;
    private final File file;

    public YamlStorage(Plugin plugin) {
        this.plugin = plugin;
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Konnte den Datenordner nicht anlegen: " + dataFolder.getPath());
        }
        this.file = new File(dataFolder, "players.yml");
    }

    @Override
    public Map<UUID, HeldenProfile> loadAll() {
        Map<UUID, HeldenProfile> profiles = new LinkedHashMap<>();
        if (!file.exists()) {
            return profiles;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = configuration.getConfigurationSection("players");
        if (root == null) {
            return profiles;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ueberspringe Profil mit ungueltiger UUID: " + key);
                continue;
            }

            HeldenProfile profile = new HeldenProfile(uuid, section.getString("name", ""));
            profile.hearts(section.getInt("hearts", 0));
            profile.eliminated(section.getBoolean("eliminated", false));
            profile.eliminatedAt(section.getLong("eliminated-at", 0L));
            profile.linkPartner(parseUuid(section.getString("link-partner", "")));
            profile.kills(section.getInt("kills", 0));
            profile.pvpDeaths(section.getInt("pvp-deaths", 0));
            profile.naturalDeaths(section.getInt("natural-deaths", 0));
            profile.lastSeen(section.getLong("last-seen", 0L));
            profiles.put(uuid, profile);
        }
        return profiles;
    }

    @Override
    public void saveAll(java.util.Collection<HeldenProfile> profiles) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (HeldenProfile profile : profiles) {
            String path = "players." + profile.uuid();
            configuration.set(path + ".name", profile.name());
            configuration.set(path + ".hearts", profile.hearts());
            configuration.set(path + ".eliminated", profile.eliminated());
            configuration.set(path + ".eliminated-at", profile.eliminatedAt());
            configuration.set(path + ".link-partner",
                    profile.linkPartner() == null ? "" : profile.linkPartner().toString());
            configuration.set(path + ".kills", profile.kills());
            configuration.set(path + ".pvp-deaths", profile.pvpDeaths());
            configuration.set(path + ".natural-deaths", profile.naturalDeaths());
            configuration.set(path + ".last-seen", profile.lastSeen());
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Konnte die Spielerdaten nicht speichern.", exception);
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
