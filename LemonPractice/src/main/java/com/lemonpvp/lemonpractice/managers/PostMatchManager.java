package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a short-lived snapshot of both players' final loadouts after a duel so
 * either side can review what the other was carrying — the "view inventories"
 * feature familiar from the big practice servers. Snapshots are held for a
 * minute after the match and looked up per viewer: each participant gets their
 * own loadout plus the opponent's.
 */
public class PostMatchManager {

    /** A captured loadout: display name, 36 main slots, 4 armor slots, offhand. */
    public record Loadout(String name, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {}

    /** What a single viewer can review after their match. */
    public record Review(Loadout own, Loadout opponent, long expiresAt) {}

    private static final long TTL_MS = 60_000L;

    private final LemonPractice plugin;
    private final Map<UUID, Review> byViewer = new ConcurrentHashMap<>();

    public PostMatchManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    /** Snapshots a player's current inventory (clones so later changes don't leak in). */
    public Loadout snapshot(Player p) {
        if (p == null) return null;
        return new Loadout(
                p.getName(),
                p.getInventory().getContents().clone(),
                p.getInventory().getArmorContents().clone(),
                p.getInventory().getItemInOffHand().clone());
    }

    /**
     * Stores the two loadouts under both participants so each can review their
     * own and the other's. Either loadout may be null (e.g. a player quit).
     */
    public void store(UUID winner, UUID loser, Loadout winnerLoadout, Loadout loserLoadout) {
        long expires = System.currentTimeMillis() + TTL_MS;
        if (winner != null) byViewer.put(winner, new Review(winnerLoadout, loserLoadout, expires));
        if (loser != null)  byViewer.put(loser,  new Review(loserLoadout, winnerLoadout, expires));
    }

    /** Returns the viewer's review if one exists and hasn't expired, else null. */
    public Review get(UUID viewer) {
        Review r = byViewer.get(viewer);
        if (r == null) return null;
        if (System.currentTimeMillis() > r.expiresAt()) {
            byViewer.remove(viewer);
            return null;
        }
        return r;
    }

    public void clear(UUID viewer) {
        byViewer.remove(viewer);
    }
}
