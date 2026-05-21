package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.FFAArena;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FFAManager {

    private static final String FFA_GAMEMODE = "sword";

    private final LemonPractice plugin;

    /** arena id -> FFAArena */
    private final Map<Integer, FFAArena> ffaArenas = new ConcurrentHashMap<>();

    /** player uuid -> arena id */
    private final Map<UUID, Integer> playerArena = new ConcurrentHashMap<>();

    public FFAManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Arena registration (called after DB load)
    // -----------------------------------------------------------------------

    public void registerArena(FFAArena arena) {
        ffaArenas.put(arena.getId(), arena);
    }

    public void loadAll() {
        plugin.getDatabase().loadFfaArenas().thenAccept(arenas -> {
            ffaArenas.clear();
            for (FFAArena a : arenas) {
                ffaArenas.put(a.getId(), a);
            }
            plugin.getLogger().info("[FFAManager] Loaded " + ffaArenas.size() + " FFA arenas.");
        });
    }

    // -----------------------------------------------------------------------
    // Join / leave
    // -----------------------------------------------------------------------

    public void joinArena(Player player, FFAArena arena) {
        if (player == null || !player.isOnline()) return;

        // Remove from any previous arena first
        if (playerArena.containsKey(player.getUniqueId())) {
            leaveArena(player);
        }

        Location spawn = arena.getRandomSpawn();
        if (spawn == null) {
            player.sendMessage("§cThis FFA arena has no spawn points configured.");
            return;
        }

        preparePlayer(player);
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);

        // Give the FFA kit
        applyFfaKit(player);

        playerArena.put(player.getUniqueId(), arena.getId());
        arena.incrementPlayers();

        plugin.getLogger().info("[FFAManager] " + player.getName() + " joined FFA arena " + arena.getName());
    }

    public void leaveArena(Player player) {
        if (player == null) return;

        Integer arenaId = playerArena.remove(player.getUniqueId());
        if (arenaId != null) {
            FFAArena arena = ffaArenas.get(arenaId);
            if (arena != null) arena.decrementPlayers();
        }

        if (player.isOnline()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);

            // Teleport to lobby
            String lobbyServer = plugin.getConfig().getString("servers.lobby", "lobby");
            plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
        }

        plugin.getLogger().info("[FFAManager] " + player.getName() + " left FFA arena.");
    }

    // -----------------------------------------------------------------------
    // Kill handling
    // -----------------------------------------------------------------------

    public void handleKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;

        // Update stats via LemonCore if available (reflective to avoid hard dependency)
        tryUpdateLemonCoreStats(killer, victim);

        // Respawn victim at a random spawn after 3 seconds
        FFAArena arena = getArena(victim.getUniqueId());
        if (arena == null) return;

        victim.setGameMode(GameMode.SPECTATOR);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!victim.isOnline()) return;
            // Check victim is still in this arena
            if (!Objects.equals(playerArena.get(victim.getUniqueId()), arena.getId())) return;

            Location respawn = arena.getRandomSpawn();
            if (respawn == null) return;

            preparePlayer(victim);
            victim.teleport(respawn);
            victim.setGameMode(GameMode.SURVIVAL);
            applyFfaKit(victim);
        }, 60L); // 3 seconds
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public FFAArena getArena(UUID playerUuid) {
        Integer id = playerArena.get(playerUuid);
        if (id == null) return null;
        return ffaArenas.get(id);
    }

    public FFAArena getArenaById(int id) {
        return ffaArenas.get(id);
    }

    public Map<Integer, FFAArena> getAllArenas() {
        return Collections.unmodifiableMap(ffaArenas);
    }

    public boolean isInFfa(UUID playerUuid) {
        return playerArena.containsKey(playerUuid);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void preparePlayer(Player player) {
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setExp(0);
        player.setLevel(0);
    }

    private void applyFfaKit(Player player) {
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), FFA_GAMEMODE);
        if (kit == null) {
            plugin.getLogger().warning("[FFAManager] No kit found for gamemode '" + FFA_GAMEMODE + "'.");
            return;
        }
        kit.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    private void tryUpdateLemonCoreStats(Player killer, Player victim) {
        try {
            // Attempt to hook into LemonCore's stats API via its plugin if available.
            // If LemonCore is not present, this block silently does nothing.
            org.bukkit.plugin.Plugin lemonCore = Bukkit.getPluginManager().getPlugin("LemonCore");
            if (lemonCore == null || !lemonCore.isEnabled()) return;

            // Reflective call so we don't create a hard compile-time dependency.
            Class<?> statsClass = Class.forName("com.lemonpvp.lemoncore.stats.StatsAPI");
            java.lang.reflect.Method recordKill = statsClass.getMethod("recordKill",
                    Player.class, Player.class);
            recordKill.invoke(null, killer, victim);
        } catch (Exception ignored) {
            // LemonCore not present or API has changed — silently skip.
        }
    }
}
