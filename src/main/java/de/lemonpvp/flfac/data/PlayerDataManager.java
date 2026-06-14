package de.lemonpvp.flfac.data;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores and hands out {@link PlayerData} for online players. */
public final class PlayerDataManager {

    private final ConcurrentHashMap<UUID, PlayerData> data = new ConcurrentHashMap<>();

    public PlayerData getOrCreate(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(player));
    }

    public PlayerData get(Player player) {
        return data.get(player.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        return data.get(uuid);
    }

    public void remove(Player player) {
        data.remove(player.getUniqueId());
    }

    public Collection<PlayerData> all() {
        return data.values();
    }

    public void clear() {
        data.clear();
    }
}
