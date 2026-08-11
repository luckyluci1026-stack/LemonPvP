package de.lemonpvp.helden.player;

import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.storage.Storage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Haelt alle Profile im Speicher und schreibt sie ueber den {@link Storage} weg. */
public final class ProfileManager {

    private final Storage storage;
    private final Settings settings;
    private final Map<UUID, HeldenProfile> profiles = new ConcurrentHashMap<>();

    public ProfileManager(Storage storage, Settings settings) {
        this.storage = storage;
        this.settings = settings;
    }

    public void loadAll() {
        profiles.clear();
        profiles.putAll(storage.loadAll());
    }

    public void saveAll() {
        storage.saveAll(profiles.values());
    }

    public HeldenProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    public HeldenProfile get(Player player) {
        return player == null ? null : profiles.get(player.getUniqueId());
    }

    /** Profil holen oder neu anlegen - mit den Startwerten aus der Config. */
    public HeldenProfile getOrCreate(Player player) {
        HeldenProfile profile = profiles.computeIfAbsent(player.getUniqueId(), uuid -> {
            HeldenProfile created = new HeldenProfile(uuid, player.getName());
            created.lives(settings.livesStart());
            created.coins(settings.startBalance());
            return created;
        });
        profile.name(player.getName());
        profile.lastSeen(System.currentTimeMillis());
        return profile;
    }

    public HeldenProfile get(OfflinePlayer player) {
        return player == null ? null : profiles.get(player.getUniqueId());
    }

    /** Sucht ein Profil ueber den zuletzt bekannten Namen (auch offline). */
    public HeldenProfile findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (HeldenProfile profile : profiles.values()) {
            if (name.equalsIgnoreCase(profile.name())) {
                return profile;
            }
        }
        return null;
    }

    public Collection<HeldenProfile> all() {
        return profiles.values();
    }

    public int size() {
        return profiles.size();
    }
}
