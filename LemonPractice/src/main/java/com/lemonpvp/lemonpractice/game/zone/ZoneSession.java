package com.lemonpvp.lemonpractice.game.zone;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One running Zone Practice round.
 *
 * Players fight in real, random vanilla terrain inside a shrinking circular zone.
 * The zone is shown to each player via a per-player {@link WorldBorder} (so they
 * see the closing ring), and a manual per-tick damage check is also run for
 * reliability. Players outside the circle take configurable damage per second.
 *
 * Self-contained: owns its own scheduler tasks and cancels them on end.
 */
public class ZoneSession {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final World world;
    private final Location center;
    private final String kitGamemode;

    private final double initialRadius;
    private final double minRadius;
    private final double damagePerSecond;
    private final int shrinkSeconds;
    private final double borderWarning;

    /** Players currently alive in the round. */
    private final Set<UUID> participants = Collections.synchronizedSet(new HashSet<>());

    private double currentRadius;
    private boolean started = false;
    private boolean ended = false;

    private BukkitTask shrinkTask;
    private BukkitTask damageTask;

    public ZoneSession(LemonPractice plugin, World world, Location center,
                       double initialRadius, double minRadius, int shrinkSeconds,
                       double damagePerSecond, String kitGamemode, double borderWarning) {
        this.plugin = plugin;
        this.world = world;
        this.center = center;
        this.initialRadius = initialRadius;
        this.minRadius = minRadius;
        this.shrinkSeconds = Math.max(1, shrinkSeconds);
        this.damagePerSecond = damagePerSecond;
        this.kitGamemode = kitGamemode;
        this.borderWarning = borderWarning;
        this.currentRadius = initialRadius;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Begins the shrink + damage tasks. Safe to call once. */
    public void start() {
        if (started || ended) return;
        started = true;

        // Shrink the radius linearly from initial to min over shrinkSeconds.
        // We tick the shrink once per second for smoothness with the border.
        final double shrinkPerSecond = (initialRadius - minRadius) / (double) shrinkSeconds;

        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (ended) return;
            if (currentRadius > minRadius) {
                currentRadius = Math.max(minRadius, currentRadius - shrinkPerSecond);
                // Refresh each player's border to follow the new radius.
                synchronized (participants) {
                    for (UUID uuid : participants) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) applyBorder(p);
                    }
                }
            }
        }, 20L, 20L);

        // Damage players outside the circle, once per second.
        damageTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyDamage, 20L, 20L);
    }

    /** Stops tasks and clears per-player borders. Idempotent. */
    public void stop() {
        ended = true;
        if (shrinkTask != null) {
            shrinkTask.cancel();
            shrinkTask = null;
        }
        if (damageTask != null) {
            damageTask.cancel();
            damageTask = null;
        }
        synchronized (participants) {
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) clearBorder(p);
            }
            participants.clear();
        }
    }

    // ------------------------------------------------------------------
    // Join / leave / elimination
    // ------------------------------------------------------------------

    /**
     * Adds the player to the round: prepares them, scatters them to a random
     * safe location inside the current zone, applies the kit and the border.
     */
    public void addPlayer(Player player) {
        if (player == null || !player.isOnline() || ended) return;

        participants.add(player.getUniqueId());

        preparePlayer(player);

        Location spawn = findSafeScatterLocation();
        if (spawn == null) {
            // Fall back to the zone center's surface.
            spawn = surfaceAt(center.getBlockX(), center.getBlockZ());
        }
        if (spawn != null) {
            player.teleport(spawn);
        }

        player.setGameMode(GameMode.SURVIVAL);
        applyKit(player);
        applyBorder(player);

        player.showTitle(Title.title(
                MM.deserialize("<gradient:#ff5555:#ffaa00><bold>ZONE</bold></gradient>"),
                MM.deserialize("<gray>Stay inside the ring. Last one standing wins!"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
        player.sendMessage(MM.deserialize(
                "<gradient:#ff5555:#ffaa00><bold>ZONE</bold></gradient> "
                + "<green>You joined the zone! <gray>(" + participants.size() + " players)"));
    }

    /** Removes a player from the round without ending it (e.g. /zone leave). */
    public void removePlayer(Player player) {
        if (player == null) return;
        participants.remove(player.getUniqueId());
        if (player.isOnline()) clearBorder(player);
    }

    /** Removes a participant by UUID (e.g. quit / death). */
    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) clearBorder(p);
    }

    public boolean contains(UUID uuid) {
        return participants.contains(uuid);
    }

    public int getPlayerCount() {
        return participants.size();
    }

    public boolean isEnded() {
        return ended;
    }

    public Set<UUID> getParticipants() {
        synchronized (participants) {
            return new HashSet<>(participants);
        }
    }

    // ------------------------------------------------------------------
    // Zone geometry / damage
    // ------------------------------------------------------------------

    private void applyDamage() {
        if (ended) return;
        synchronized (participants) {
            for (UUID uuid : participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                if (player.isDead()) continue;
                if (!isInside(player.getLocation())) {
                    player.damage(damagePerSecond);
                    player.sendActionBar(MM.deserialize(
                            "<red><bold>⚠ Outside the zone! Get back in! ⚠</bold></red>"));
                } else {
                    double dist = horizontalDistance(player.getLocation());
                    if (dist > currentRadius - 8) {
                        player.sendActionBar(MM.deserialize(
                                "<yellow>The zone is closing — " + (int) currentRadius + "m radius"));
                    }
                }
            }
        }
    }

    public boolean isInside(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().equals(world)) return false;
        return horizontalDistance(location) <= currentRadius;
    }

    private double horizontalDistance(Location location) {
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double getCurrentRadius() {
        return currentRadius;
    }

    // ------------------------------------------------------------------
    // Per-player world border (visual ring)
    // ------------------------------------------------------------------

    private void applyBorder(Player player) {
        // Always use a fresh per-player border so we never mutate the world's
        // real border (player.getWorldBorder() may return the world border).
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(center.getX(), center.getZ());
        // WorldBorder size is the full diameter.
        border.setSize(Math.max(1.0, currentRadius * 2.0));
        border.setWarningDistance((int) Math.max(0, borderWarning));
        border.setDamageAmount(0.0); // we handle damage manually
        player.setWorldBorder(border);
    }

    private void clearBorder(Player player) {
        // Resetting to null restores the world's real border.
        player.setWorldBorder(null);
    }

    // ------------------------------------------------------------------
    // Safe spawn scatter
    // ------------------------------------------------------------------

    /**
     * Picks a random point inside the current zone and returns a safe surface
     * location there (highest non-air block, avoiding lava/water if possible).
     * Tries several candidates before giving up.
     */
    private Location findSafeScatterLocation() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double usableRadius = Math.max(1.0, currentRadius - 5.0);

        Location fallback = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rng.nextDouble(0, Math.PI * 2);
            double r = usableRadius * Math.sqrt(rng.nextDouble());
            int x = (int) Math.round(center.getX() + r * Math.cos(angle));
            int z = (int) Math.round(center.getZ() + r * Math.sin(angle));

            Location surface = surfaceAt(x, z);
            if (surface == null) continue;

            Block ground = surface.clone().subtract(0, 1, 0).getBlock();
            Material groundType = ground.getType();
            if (groundType == Material.LAVA) {
                continue; // never spawn on lava
            }
            if (groundType == Material.WATER) {
                if (fallback == null) fallback = surface; // remember as last resort
                continue; // prefer dry land
            }
            return surface;
        }
        return fallback;
    }

    /**
     * Returns a standing location on top of the highest non-air block at the
     * given column, or {@code null} if the column is empty.
     */
    private Location surfaceAt(int x, int z) {
        if (world == null) return null;
        // getHighestBlockYAt uses MOTION_BLOCKING heuristics; fine for scatter.
        int y = world.getHighestBlockYAt(x, z);
        Block highest = world.getBlockAt(x, y, z);
        if (highest.getType() == Material.AIR) {
            return null;
        }
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    // ------------------------------------------------------------------
    // Player prep / kit
    // ------------------------------------------------------------------

    private void preparePlayer(Player player) {
        double max = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        player.setHealth(max);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setExp(0);
        player.setLevel(0);
    }

    private void applyKit(Player player) {
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), kitGamemode);
        if (kit == null) {
            plugin.getLogger().warning("[ZoneSession] No kit found for gamemode '" + kitGamemode + "'.");
            return;
        }
        kit.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    public World getWorld() {
        return world;
    }
}
