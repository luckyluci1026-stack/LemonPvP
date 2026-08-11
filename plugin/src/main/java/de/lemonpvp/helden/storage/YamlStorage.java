package de.lemonpvp.helden.storage;

import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
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
            profile.heroId(emptyToNull(section.getString("hero", "")));
            profile.teamId(emptyToNull(section.getString("team", "")));
            profile.heroSelectedAt(section.getLong("hero-selected-at", 0L));
            profile.lives(section.getInt("lives", 0));
            profile.fallen(section.getBoolean("fallen", false));
            profile.kills(section.getInt("kills", 0));
            profile.deaths(section.getInt("deaths", 0));
            profile.assists(section.getInt("assists", 0));
            profile.killStreak(section.getInt("kill-streak", 0));
            profile.bestKillStreak(section.getInt("best-kill-streak", 0));
            profile.coins(section.getInt("coins", 0));
            profile.lastSeen(section.getLong("last-seen", 0L));
            profiles.put(uuid, profile);
        }
        return profiles;
    }

    @Override
    public void saveAll(Collection<HeldenProfile> profiles) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (HeldenProfile profile : profiles) {
            String path = "players." + profile.uuid();
            configuration.set(path + ".name", profile.name());
            configuration.set(path + ".hero", profile.heroId() == null ? "" : profile.heroId());
            configuration.set(path + ".team", profile.teamId() == null ? "" : profile.teamId());
            configuration.set(path + ".hero-selected-at", profile.heroSelectedAt());
            configuration.set(path + ".lives", profile.lives());
            configuration.set(path + ".fallen", profile.fallen());
            configuration.set(path + ".kills", profile.kills());
            configuration.set(path + ".deaths", profile.deaths());
            configuration.set(path + ".assists", profile.assists());
            configuration.set(path + ".kill-streak", profile.killStreak());
            configuration.set(path + ".best-kill-streak", profile.bestKillStreak());
            configuration.set(path + ".coins", profile.coins());
            configuration.set(path + ".last-seen", profile.lastSeen());
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Konnte die Spielerdaten nicht speichern.", exception);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
