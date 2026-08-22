package de.lemonpvp.helden.player;

import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.storage.Storage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Haelt alle Profile im Speicher und schreibt sie ueber den {@link Storage} weg. */
public final class ProfileManager {

    private final Storage storage;
    private final Settings settings;
    private final ConcurrentHashMap<UUID, HeldenProfile> profiles = new ConcurrentHashMap<>();

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
        return uuid == null ? null : profiles.get(uuid);
    }

    public HeldenProfile get(Player player) {
        return player == null ? null : profiles.get(player.getUniqueId());
    }

    /** Profil holen oder neu anlegen - neue Spieler starten mit vollen Herzen. */
    public HeldenProfile getOrCreate(Player player) {
        HeldenProfile profile = profiles.computeIfAbsent(player.getUniqueId(), uuid -> {
            HeldenProfile created = new HeldenProfile(uuid, player.getName());
            created.hearts(settings.totalStartHearts());
            return created;
        });
        profile.name(player.getName());
        profile.lastSeen(System.currentTimeMillis());
        return profile;
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

    /**
     * Alle Profile in Anzeigereihenfolge: noch im Rennen zuerst, dann nach
     * Herzen und Kills. Wird von der Teilnehmerliste und dem Befehl geteilt.
     */
    public List<HeldenProfile> ranked() {
        Comparator<HeldenProfile> order = Comparator
                .<HeldenProfile, Boolean>comparing(HeldenProfile::eliminated)
                .thenComparing(HeldenProfile::hearts, Comparator.reverseOrder())
                .thenComparing(HeldenProfile::kills, Comparator.reverseOrder())
                .thenComparing(HeldenProfile::name, Comparator.naturalOrder());

        List<HeldenProfile> list = new ArrayList<>(profiles.values());
        list.sort(order);
        return list;
    }

    public Collection<HeldenProfile> all() {
        return profiles.values();
    }

    public int size() {
        return profiles.size();
    }
}
