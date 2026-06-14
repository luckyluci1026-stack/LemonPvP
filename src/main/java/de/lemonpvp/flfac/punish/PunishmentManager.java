package de.lemonpvp.flfac.punish;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.Bukkit;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes configured console commands once a player's violation level for a
 * check reaches a threshold. Each threshold fires at most once per session.
 */
public final class PunishmentManager {

    private final FLFAC plugin;
    private final Map<UUID, EnumMap<CheckType, Integer>> executed = new ConcurrentHashMap<>();

    public PunishmentManager(FLFAC plugin) {
        this.plugin = plugin;
    }

    public void handle(CheckType type, PlayerData data, double vl) {
        var ladder = plugin.getConfigManager().getLadder(type);
        if (ladder.isEmpty()) {
            return;
        }

        EnumMap<CheckType, Integer> playerMap = executed.computeIfAbsent(data.getUuid(), u -> new EnumMap<>(CheckType.class));
        int lastExecuted = playerMap.getOrDefault(type, -1);

        for (Map.Entry<Integer, String> entry : ladder.entrySet()) {
            int threshold = entry.getKey();
            if (threshold <= vl && threshold > lastExecuted) {
                runCommand(entry.getValue(), type, data, vl);
                playerMap.put(type, threshold);
                lastExecuted = threshold;
            }
        }
    }

    private void runCommand(String raw, CheckType type, PlayerData data, double vl) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String command = raw
                .replace("%player%", data.getName())
                .replace("%check%", type.getDisplayName())
                .replace("%vl%", String.valueOf((int) Math.floor(vl)));
        // Always run from console on the main thread.
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }

    public void clear(UUID uuid) {
        executed.remove(uuid);
    }

    public void clearAll() {
        executed.clear();
    }
}
