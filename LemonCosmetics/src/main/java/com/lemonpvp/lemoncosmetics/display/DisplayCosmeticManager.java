package com.lemonpvp.lemoncosmetics.display;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns every player's packet Display cosmetics and keeps them glued to the wearer.
 *
 * <p>Three loops cooperate:</p>
 * <ul>
 *   <li><b>reconcile</b> (main thread, every {@value #RECONCILE_TICKS} ticks) —
 *       works out who is in tracking range and spawns/despawns the cosmetics for
 *       them, so players see them appear/disappear like real entities.</li>
 *   <li><b>animate</b> (main thread, every {@value #ANIM_TICKS} ticks) — sends a
 *       fresh rotation transform for capes/wings with {@code interpolation_duration}
 *       equal to the interval, so the vanilla client tweens it smoothly (task 5).</li>
 *   <li><b>{@link #onMovement}</b> (netty thread, from {@link CosmeticMoveListener})
 *       — the moment a movement packet arrives, before the tick ends, it teleports
 *       the cosmetics to the new position so they never lag behind (task 4).</li>
 * </ul>
 *
 * <p>To stay thread-safe, {@link #onMovement} never touches live Bukkit entity
 * state: it uses the coordinates from the packet plus a cached {@link World} and
 * last yaw, and sends packets (which PacketEvents allows off-thread) to a cached
 * set of viewer {@link Player}s.</p>
 */
public class DisplayCosmeticManager {

    private static final int RECONCILE_TICKS = 10;
    private static final int ANIM_TICKS = 5;
    private static final double TRACK_RANGE = 48.0;
    private static final double TRACK_RANGE_SQ = TRACK_RANGE * TRACK_RANGE;

    private final org.bukkit.plugin.Plugin plugin;

    private final Map<java.util.UUID, List<DisplayCosmetic>> worn = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Set<Player>> viewers = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, World> wearerWorld = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Float> wearerYaw = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, double[]> wearerPos = new ConcurrentHashMap<>(); // last x,y,z

    private int animPhase = 0;

    public DisplayCosmeticManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::reconcile, RECONCILE_TICKS, RECONCILE_TICKS);
        Bukkit.getScheduler().runTaskTimer(plugin, this::animate, ANIM_TICKS, ANIM_TICKS);
    }

    // ── Equip / unequip ─────────────────────────────────────────────────────

    /** Gives the wearer a cosmetic; it appears to everyone tracking them. */
    public void equip(Player wearer, DisplayCosmetic cosmetic) {
        worn.computeIfAbsent(wearer.getUniqueId(), k -> new CopyOnWriteArrayList<>()).add(cosmetic);
        wearerWorld.put(wearer.getUniqueId(), wearer.getWorld());
        wearerYaw.put(wearer.getUniqueId(), wearer.getLocation().getYaw());
        // Spawn immediately for whoever already tracks them.
        for (Player viewer : viewersOf(wearer.getUniqueId())) cosmetic.spawn(viewer, wearer.getLocation());
    }

    /** Removes all of a wearer's cosmetics (e.g. on unequip or quit). */
    public void clear(java.util.UUID wearer) {
        List<DisplayCosmetic> cs = worn.remove(wearer);
        Set<Player> vs = viewers.remove(wearer);
        wearerWorld.remove(wearer);
        wearerYaw.remove(wearer);
        wearerPos.remove(wearer);
        if (cs == null || vs == null) return;
        for (Player viewer : vs) for (DisplayCosmetic c : cs) c.remove(viewer);
    }

    public boolean hasCosmetics(java.util.UUID wearer) {
        List<DisplayCosmetic> cs = worn.get(wearer);
        return cs != null && !cs.isEmpty();
    }

    // ── Task 4: low-latency position sync (called from the packet listener) ──

    /**
     * Teleports a wearer's cosmetics to a freshly-received position. Runs on the
     * netty thread — packet sends are safe there, and we avoid Bukkit entity reads
     * by using the packet coords + cached world/yaw.
     *
     * @param yaw the yaw from the packet, or {@code null} for a position-only move
     */
    public void onMovement(java.util.UUID wearer, double x, double y, double z, Float yaw) {
        wearerPos.put(wearer, new double[]{x, y, z});
        dispatchMove(wearer, x, y, z, yaw);
    }

    /**
     * A rotation-only packet (the player turned in place): reuse the last known
     * position so a cape/wings still swings round to the new facing.
     */
    public void onRotation(java.util.UUID wearer, float yaw) {
        double[] pos = wearerPos.get(wearer);
        if (pos == null) return;
        dispatchMove(wearer, pos[0], pos[1], pos[2], yaw);
    }

    private void dispatchMove(java.util.UUID wearer, double x, double y, double z, Float yaw) {
        List<DisplayCosmetic> cs = worn.get(wearer);
        if (cs == null || cs.isEmpty()) return;
        Set<Player> vs = viewers.get(wearer);
        if (vs == null || vs.isEmpty()) return;
        World world = wearerWorld.get(wearer);
        if (world == null) return;

        float useYaw = yaw != null ? yaw : wearerYaw.getOrDefault(wearer, 0f);
        if (yaw != null) wearerYaw.put(wearer, yaw);

        Location wl = new Location(world, x, y, z, useYaw, 0f);
        for (Player viewer : vs) {
            if (viewer == null || !viewer.isOnline()) continue;
            for (DisplayCosmetic c : cs) c.move(viewer, wl);
        }
    }

    // ── Reconcile trackers (main thread) ────────────────────────────────────

    private void reconcile() {
        for (Map.Entry<java.util.UUID, List<DisplayCosmetic>> e : worn.entrySet()) {
            Player wearer = Bukkit.getPlayer(e.getKey());
            if (wearer == null || !wearer.isOnline()) continue;
            wearerWorld.put(wearer.getUniqueId(), wearer.getWorld());

            Set<Player> current = viewersOf(wearer.getUniqueId());
            Location wl = wearer.getLocation();

            // Spawn for players newly in range.
            for (Player nearby : wearer.getWorld().getPlayers()) {
                if (nearby.getLocation().distanceSquared(wl) > TRACK_RANGE_SQ) continue;
                if (current.add(nearby)) {
                    for (DisplayCosmetic c : e.getValue()) c.spawn(nearby, wl);
                }
            }
            // Despawn for players who left range or worlds.
            current.removeIf(viewer -> {
                boolean gone = !viewer.isOnline() || viewer.getWorld() != wearer.getWorld()
                        || viewer.getLocation().distanceSquared(wl) > TRACK_RANGE_SQ;
                if (gone) for (DisplayCosmetic c : e.getValue()) c.remove(viewer);
                return gone;
            });
        }
    }

    // ── Task 5: interpolated animation (main thread) ────────────────────────

    private void animate() {
        animPhase++;
        double t = animPhase * 0.4;
        for (Map.Entry<java.util.UUID, List<DisplayCosmetic>> e : worn.entrySet()) {
            Set<Player> vs = viewers.get(e.getKey());
            if (vs == null || vs.isEmpty()) continue;
            for (DisplayCosmetic c : e.getValue()) {
                DisplayCosmeticPackets.Transform swung = swingFor(c, t);
                if (swung == null) continue; // hats don't sway
                for (Player viewer : vs) {
                    if (viewer.isOnline()) c.animate(viewer, swung, ANIM_TICKS);
                }
            }
        }
    }

    /**
     * Builds the swaying transform for a cosmetic at animation time {@code t}. The
     * cape rocks a few degrees about its back axis; wings flap about the vertical
     * axis. Returns {@code null} for slots that don't animate (hats). Sent with an
     * {@code interpolation_duration} equal to the send interval so the client fills
     * the in-between frames itself (task 5).
     */
    private DisplayCosmeticPackets.Transform swingFor(DisplayCosmetic c, double t) {
        float ax, ay, az, center, amplitude;
        switch (c.slot()) {
            case CAPE -> { ax = 0; ay = 0; az = 1; center = 12f; amplitude = 6f; }
            case BACKPACK -> { ax = 0; ay = 1; az = 0; center = 0f; amplitude = 18f; }
            default -> { return null; }
        }
        float angle = center + amplitude * (float) Math.sin(t);
        DisplayCosmeticPackets.Transform base = c.baseTransform();
        return new DisplayCosmeticPackets.Transform(
                base.translation(), base.scale(),
                DisplayCosmeticPackets.axisAngle(ax, ay, az, angle),
                DisplayCosmeticPackets.noRotation());
    }

    private Set<Player> viewersOf(java.util.UUID wearer) {
        return viewers.computeIfAbsent(wearer, k -> ConcurrentHashMap.newKeySet());
    }
}
