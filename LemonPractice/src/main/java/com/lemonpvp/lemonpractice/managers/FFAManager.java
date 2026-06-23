package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.FFAArena;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FFAManager {

    private static final String FFA_GAMEMODE = "sword";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    /** arena id -> FFAArena */
    private final Map<Integer, FFAArena> ffaArenas = new ConcurrentHashMap<>();

    /** player uuid -> arena id */
    private final Map<UUID, Integer> playerArena = new ConcurrentHashMap<>();

    /** Session FFA stats (reset when the player leaves the arena). */
    private final Map<UUID, Integer> sessionKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sessionKillstreak = new ConcurrentHashMap<>();

    public FFAManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the repeating FFA scoreboard + action bar updater. Call once on enable.
     */
    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : playerArena.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                renderScoreboard(p);
                p.sendActionBar(MM.deserialize(
                        "<gray>❤ <red>" + String.format("%.1f", p.getHealth())
                        + " <dark_gray>| <gray>Killstreak <gold>"
                        + sessionKillstreak.getOrDefault(uuid, 0)));
            }
        }, 20L, 20L);
    }

    private void renderScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        FFAArena arena = getArena(uuid);
        if (arena == null) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("ffa", Criteria.DUMMY,
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>Fꜰᴀ</bold></gradient>"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Component> lines = new ArrayList<>();
        lines.add(MM.deserialize("<dark_gray><st>                </st>"));
        lines.add(MM.deserialize("<gray>Kills: <green>" + sessionKills.getOrDefault(uuid, 0)));
        lines.add(MM.deserialize("<gray>Killstreak: <gold>" + sessionKillstreak.getOrDefault(uuid, 0)));
        lines.add(Component.empty());
        lines.add(MM.deserialize("<gray>Spieler: <white>" + arena.getPlayerCount()));
        lines.add(MM.deserialize("<gray>Arena: <white>" + arena.getName()));
        lines.add(MM.deserialize("<dark_gray><st>                </st>"));

        for (int i = 0; i < lines.size(); i++) {
            int scoreValue = lines.size() - i;
            String entryKey = " ".repeat(i + 1);
            Score score = obj.getScore(entryKey);
            score.setScore(scoreValue);
            score.customName(lines.get(i));
        }

        player.setScoreboard(board);
    }

    /**
     * FlowPvP-style FFA entrance: gradient title, swoosh sound sequence,
     * and a ring of yellow/green Redstone particles at spawn.
     */
    private void showFfaEntrance(Player player, FFAArena arena) {
        // Title — big gradient main, subtitle with arena name
        Title title = Title.title(
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>Fꜰᴀ</bold></gradient>"),
                MM.deserialize("<gray>Arena: <white>" + arena.getName()
                        + "  <dark_gray>|  <gray>Spieler: <green>" + arena.getPlayerCount()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500))
        );
        player.showTitle(title);

        // Sound sequence: whoosh → ding
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline())
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);
        }, 6L);

        // Particle ring at spawn position — yellow + green dust
        Location loc = player.getLocation();
        int count = 24;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = loc.getX() + 1.2 * Math.cos(angle);
            double z = loc.getZ() + 1.2 * Math.sin(angle);
            Location pLoc = new Location(loc.getWorld(), x, loc.getY() + 0.1, z);
            loc.getWorld().spawnParticle(Particle.DUST,
                    pLoc, 2, 0, 0, 0, 0,
                    new Particle.DustOptions(i % 2 == 0 ? Color.YELLOW : Color.LIME, 1.2f));
        }

        // Chat message
        player.sendMessage(MM.deserialize(
                "<gradient:#fffb00:#00ff00><bold>FFA</bold></gradient> "
                + "<green>Du bist <white>" + arena.getName() + " <green>beigetreten!  "
                + "<dark_gray>(<gray>" + arena.getPlayerCount() + " Spieler<dark_gray>)"));
    }

    /** Resets the FFA killstreak for a player (called on their death). */
    public void resetKillstreak(UUID uuid) {
        sessionKillstreak.put(uuid, 0);
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

        // Initialise session stats
        sessionKills.put(player.getUniqueId(), 0);
        sessionKillstreak.put(player.getUniqueId(), 0);

        showFfaEntrance(player, arena);

        plugin.getLogger().info("[FFAManager] " + player.getName() + " joined FFA arena " + arena.getName());
    }

    public void leaveArena(Player player) {
        if (player == null) return;

        Integer arenaId = playerArena.remove(player.getUniqueId());
        if (arenaId != null) {
            FFAArena arena = ffaArenas.get(arenaId);
            if (arena != null) arena.decrementPlayers();
        }

        // Clear session stats and reset the scoreboard
        sessionKills.remove(player.getUniqueId());
        sessionKillstreak.remove(player.getUniqueId());

        if (player.isOnline()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            // Teleport to lobby
            String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
            plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
        }

        plugin.getLogger().info("[FFAManager] " + player.getName() + " left FFA arena.");
    }

    // -----------------------------------------------------------------------
    // Kill handling
    // -----------------------------------------------------------------------

    public void handleKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;

        // Increment session stats for the killer
        UUID killerUuid = killer.getUniqueId();
        int kills = sessionKills.merge(killerUuid, 1, Integer::sum);
        int streak = sessionKillstreak.merge(killerUuid, 1, Integer::sum);

        // Heal + feedback for the killer
        killer.setHealth(Objects.requireNonNull(
                killer.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        killer.setFoodLevel(20);
        killer.playSound(killer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        killer.sendMessage(MM.deserialize("<gray>Du hast <red>" + victim.getName()
                + "</red> getötet! <dark_gray>(<green>" + kills + " Kills</green>)"));

        // Killstreak milestone announcement to the whole arena
        if (streak % 5 == 0) {
            broadcastArena(getArena(killerUuid), MM.deserialize(
                    "<gold>" + killer.getName() + "</gold> <yellow>ist auf einem <gold>"
                    + streak + "</gold> Killstreak!"));
        }

        // Update stats via LemonCore if available (reflective to avoid hard dependency).
        tryUpdateLemonCoreStats(killer, victim);
    }

    /** Broadcasts a message to every player in the given arena. */
    private void broadcastArena(FFAArena arena, Component message) {
        if (arena == null) return;
        for (Map.Entry<UUID, Integer> entry : playerArena.entrySet()) {
            if (entry.getValue() != arena.getId()) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) p.sendMessage(message);
        }
    }

    /**
     * Restores health/food and re-applies the FFA kit. Called from the respawn
     * listener so every FFA death — whether by a killer or the environment —
     * flows through one consistent code path (and the kit is always restored).
     */
    public void respawnEquip(Player player) {
        if (player == null) return;
        preparePlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        applyFfaKit(player);
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
