package com.lemonpvp.lemonpractice.game.zone;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the active Zone Practice round.
 *
 * Keeps it simple: a single rolling {@link ZoneSession} that players can join.
 * A fresh session (with a new random center) is created whenever none exists or
 * the current one has ended. The round ends (and survivors are returned to the
 * lobby) when only one player remains or the timeout elapses.
 *
 * All config values are read live from {@code plugin.getConfig()} under "zone:".
 */
public class ZonePracticeManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    private ZoneSession active;

    public ZonePracticeManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Config accessors
    // ------------------------------------------------------------------

    private String worldName() { return plugin.getConfig().getString("zone.world", "world"); }
    private double initialRadius() { return plugin.getConfig().getDouble("zone.initial-radius", 200.0); }
    private double minRadius() { return plugin.getConfig().getDouble("zone.min-radius", 10.0); }
    private int shrinkSeconds() { return plugin.getConfig().getInt("zone.shrink-seconds", 180); }
    private double damagePerSecond() { return plugin.getConfig().getDouble("zone.damage-per-second", 2.0); }
    private String kitGamemode() { return plugin.getConfig().getString("zone.kit-gamemode", "sword"); }
    private int minPlayers() { return plugin.getConfig().getInt("zone.min-players", 1); }
    private double borderWarning() { return plugin.getConfig().getDouble("zone.border-warning", 10.0); }

    // ------------------------------------------------------------------
    // Join / leave
    // ------------------------------------------------------------------

    public void join(Player player) {
        if (player == null || !player.isOnline()) return;

        if (isInZone(player.getUniqueId())) {
            player.sendMessage(MM.deserialize("<yellow>You are already in the zone."));
            return;
        }

        ZoneSession session = getOrCreateSession();
        if (session == null) {
            player.sendMessage(MM.deserialize("<red>The zone world is not loaded. Tell an admin."));
            return;
        }

        session.addPlayer(player);

        // Start the round once we have enough players and it isn't running yet.
        if (session.getPlayerCount() >= minPlayers()) {
            session.start();
        }
    }

    public void leave(Player player) {
        if (player == null) return;
        ZoneSession session = active;
        if (session == null || !session.contains(player.getUniqueId())) {
            player.sendMessage(MM.deserialize("<red>You are not in the zone."));
            return;
        }
        session.removePlayer(player);
        returnToLobby(player);
        player.sendMessage(MM.deserialize("<gray>You left the zone."));
        checkEnd();
    }

    /**
     * Eliminates a participant (death / quit). Returns true if they were in the
     * zone. The caller is responsible for returning the player to the lobby for
     * the death path (handled in the listener after respawn).
     */
    public boolean eliminate(UUID uuid) {
        ZoneSession session = active;
        if (session == null || !session.contains(uuid)) return false;
        session.removeParticipant(uuid);
        checkEnd();
        return true;
    }

    // ------------------------------------------------------------------
    // Round management
    // ------------------------------------------------------------------

    private ZoneSession getOrCreateSession() {
        if (active != null && !active.isEnded()) {
            return active;
        }
        World world = Bukkit.getWorld(worldName());
        if (world == null) {
            plugin.getLogger().warning("[ZonePracticeManager] Zone world '" + worldName() + "' is not loaded.");
            return null;
        }

        Location center = randomCenter(world);
        active = new ZoneSession(plugin, world, center,
                initialRadius(), minRadius(), shrinkSeconds(),
                damagePerSecond(), kitGamemode(), borderWarning());

        // Hard timeout: end the round shortly after the zone fully closes.
        int timeoutSeconds = shrinkSeconds() + 30;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active != null && !active.isEnded()) {
                endRound("<yellow>Time is up! The zone round has ended.");
            }
        }, timeoutSeconds * 20L);

        return active;
    }

    /**
     * Picks a random center near the world spawn so the zone lands on real,
     * loaded-ish terrain rather than far-out unexplored chunks.
     */
    private Location randomCenter(World world) {
        Location base = world.getSpawnLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Offset up to ~initialRadius blocks from spawn in each axis.
        int range = (int) Math.max(50, initialRadius());
        int x = base.getBlockX() + rng.nextInt(-range, range + 1);
        int z = base.getBlockZ() + rng.nextInt(-range, range + 1);
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    /** Ends the round if one (or zero) participants remain. */
    private void checkEnd() {
        ZoneSession session = active;
        if (session == null || session.isEnded()) return;
        if (session.getPlayerCount() <= 1) {
            // Announce the winner if there is exactly one.
            UUID winnerUuid = session.getParticipants().stream().findFirst().orElse(null);
            String winnerMsg;
            if (winnerUuid != null) {
                Player winner = Bukkit.getPlayer(winnerUuid);
                String name = winner != null ? winner.getName() : "Unknown";
                winnerMsg = "<gold><bold>" + name + "</bold></gold> <green>won the zone!";
            } else {
                winnerMsg = "<yellow>The zone round has ended.";
            }
            endRound(winnerMsg);
        }
    }

    private void endRound(String message) {
        ZoneSession session = active;
        if (session == null) return;

        for (UUID uuid : session.getParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(MM.deserialize(
                        "<gradient:#ff5555:#ffaa00><bold>ZONE</bold></gradient> " + message));
                returnToLobby(p);
            }
        }

        session.stop();
        active = null;
    }

    // ------------------------------------------------------------------
    // Lobby restoration
    // ------------------------------------------------------------------

    /**
     * Returns a player to the lobby state on this server: clears the zone kit,
     * restores health, gives the lobby hotbar and teleports to the configured
     * lobby spawn (falling back to the world spawn).
     */
    public void returnToLobby(Player player) {
        if (player == null || !player.isOnline()) return;

        // Restore the player's real world border.
        player.setWorldBorder(null);

        try {
            double max = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
            player.setHealth(max);
        } catch (Exception ignored) {
            // attribute missing — ignore
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setGameMode(GameMode.ADVENTURE);

        // Teleport to the lobby spawn if configured, else world spawn.
        Location dest = lobbySpawn();
        if (dest != null) {
            player.teleport(dest);
        }

        // Give the lobby hotbar back.
        plugin.getLobbyHotbarManager().setupHotbar(player);
    }

    private Location lobbySpawn() {
        String section = "lobby.spawn";
        if (plugin.getConfig().contains(section)) {
            String worldName = plugin.getConfig().getString(section + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world != null) {
                double x = plugin.getConfig().getDouble(section + ".x");
                double y = plugin.getConfig().getDouble(section + ".y");
                double z = plugin.getConfig().getDouble(section + ".z");
                float yaw = (float) plugin.getConfig().getDouble(section + ".yaw", 0);
                float pitch = (float) plugin.getConfig().getDouble(section + ".pitch", 0);
                return new Location(world, x, y, z, yaw, pitch);
            }
        }
        World fallback = Bukkit.getWorld(plugin.getConfig().getString("worlds.lobby", "world"));
        return fallback != null ? fallback.getSpawnLocation() : null;
    }

    // ------------------------------------------------------------------
    // Queries / cleanup
    // ------------------------------------------------------------------

    public boolean isInZone(UUID uuid) {
        return active != null && !active.isEnded() && active.contains(uuid);
    }

    public ZoneSession getActiveSession() {
        return active;
    }

    /** Cancels everything on plugin disable. */
    public void shutdown() {
        if (active != null) {
            active.stop();
            active = null;
        }
    }
}
