package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Roaming-zone FFA.
 *
 * <p>Unlike Zone Practice (a shrinking ring), the FFA is a single <b>fixed-size</b>
 * square play area — a per-player {@link WorldBorder}, {@code ffa.size} blocks
 * across (default 150) — that does <b>not</b> shrink. Every
 * {@code ffa.relocate-minutes} (default 20) the whole zone jumps to a new random
 * location and every fighter is scattered into it with the border re-centered,
 * keeping the terrain fresh. Players respawn inside the current zone on death.
 *
 * <p>There is one global zone shared by all FFA players. Config lives under
 * {@code ffa:} in config.yml. The world is expected to be provided by
 * Multiverse-Core (default {@code world_ffa}).
 */
public class FFAManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    /** Everyone currently in the FFA. */
    private final Set<UUID> participants = Collections.synchronizedSet(new HashSet<>());

    /** Session FFA stats (reset when the player leaves). */
    private final Map<UUID, Integer> sessionKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sessionKillstreak = new ConcurrentHashMap<>();

    /** The world the zone lives in (resolved lazily; Multiverse may load it late). */
    private World world;
    /** Current zone center; null until the first player joins. */
    private Location currentCenter;
    /** Seconds left before the zone relocates. */
    private int secondsUntilRelocate;

    private BukkitTask hudTask;

    public FFAManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Config accessors (read live so /lpractice reload picks them up)
    // -----------------------------------------------------------------------

    private String worldName()      { return plugin.getConfig().getString("ffa.world", "world_ffa"); }
    private double size()           { return Math.max(16, plugin.getConfig().getDouble("ffa.size", 150)); }
    private int relocateMinutes()   { return Math.max(1, plugin.getConfig().getInt("ffa.relocate-minutes", 20)); }
    private int centerRange()       { return Math.max(50, plugin.getConfig().getInt("ffa.center-range", 2000)); }
    private String kitGamemode()    { return plugin.getConfig().getString("ffa.kit-gamemode", "sword"); }
    private int respawnDelayTicks() { return Math.max(1, plugin.getConfig().getInt("ffa.respawn-delay-ticks", 60)); }
    private int borderWarning()     { return Math.max(0, plugin.getConfig().getInt("ffa.border-warning", 6)); }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Resolves the FFA world (if already loaded). Safe to call before Multiverse loads it. */
    public void loadAll() {
        world = Bukkit.getWorld(worldName());
        if (world != null) {
            currentCenter = randomCenter();
            plugin.getLogger().info("[FFAManager] Roaming FFA zone ready in world '" + worldName() + "'.");
        } else {
            plugin.getLogger().info("[FFAManager] FFA world '" + worldName()
                    + "' not loaded yet — it will be resolved on first join.");
        }
    }

    /** Starts the per-second HUD + relocation task. Call once on enable. */
    public void startTasks() {
        secondsUntilRelocate = relocateMinutes() * 60;
        hudTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /** Cancels tasks and clears every player's border. Called on disable. */
    public void shutdown() {
        if (hudTask != null) { hudTask.cancel(); hudTask = null; }
        for (UUID uuid : snapshot()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.setWorldBorder(null);
        }
        participants.clear();
    }

    /** Lazily resolves and returns the FFA world, or {@code null} if not loaded. */
    private World world() {
        if (world == null) world = Bukkit.getWorld(worldName());
        return world;
    }

    private List<UUID> snapshot() {
        synchronized (participants) {
            return new ArrayList<>(participants);
        }
    }

    // -----------------------------------------------------------------------
    // Per-second tick: relocation countdown + HUD
    // -----------------------------------------------------------------------

    private void tick() {
        World w = world();
        if (!participants.isEmpty() && w != null) {
            secondsUntilRelocate--;
            if (secondsUntilRelocate == 30 || secondsUntilRelocate == 10
                    || (secondsUntilRelocate <= 3 && secondsUntilRelocate > 0)) {
                warnRelocation(secondsUntilRelocate);
            }
            if (secondsUntilRelocate <= 0) {
                relocate();
                secondsUntilRelocate = relocateMinutes() * 60;
            }
        } else {
            secondsUntilRelocate = relocateMinutes() * 60;
        }

        for (UUID uuid : snapshot()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            renderScoreboard(p);
            p.sendActionBar(MM.deserialize(
                    "<gray>❤ <red>" + String.format("%.1f", p.getHealth())
                    + " <dark_gray>| <gray>Killstreak <gold>"
                    + sessionKillstreak.getOrDefault(uuid, 0)
                    + " <dark_gray>| <gray>Zone moves in <yellow>" + formatTime(secondsUntilRelocate)));
        }
    }

    private void warnRelocation(int seconds) {
        Component sub = MM.deserialize("<gray>Zone relocates in <yellow>" + seconds + "s<gray>!");
        for (UUID uuid : snapshot()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            p.sendActionBar(MM.deserialize("<gold><bold>⚠ Zone moving in " + seconds + "s ⚠</bold></gold>"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.8f);
        }
    }

    /** Picks a fresh random center and scatters everyone into the new zone. */
    private void relocate() {
        World w = world();
        if (w == null) return;
        currentCenter = randomCenter();

        for (UUID uuid : snapshot()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            Location spawn = scatterLocation();
            if (spawn != null) p.teleport(spawn);
            preparePlayer(p);
            p.setGameMode(GameMode.SURVIVAL);
            applyFfaKit(p);
            applyBorder(p);
            p.showTitle(Title.title(
                    MM.deserialize("<gradient:#fffb00:#00ff00><bold>ZONE MOVED</bold></gradient>"),
                    MM.deserialize("<gray>A fresh battleground — fight on!"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        }
    }

    // -----------------------------------------------------------------------
    // Join / leave
    // -----------------------------------------------------------------------

    public void join(Player player) {
        if (player == null || !player.isOnline()) return;
        if (world() == null) {
            player.sendMessage(MM.deserialize("<red>The FFA world is not loaded. Tell an admin."));
            return;
        }
        if (participants.contains(player.getUniqueId())) {
            player.sendMessage(MM.deserialize("<yellow>You are already in the FFA."));
            return;
        }
        if (currentCenter == null) currentCenter = randomCenter();

        participants.add(player.getUniqueId());
        sessionKills.put(player.getUniqueId(), 0);
        sessionKillstreak.put(player.getUniqueId(), 0);

        preparePlayer(player);
        Location spawn = scatterLocation();
        if (spawn != null) player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        applyFfaKit(player);
        applyBorder(player);

        showFfaEntrance(player);
        plugin.getLogger().info("[FFAManager] " + player.getName() + " joined the FFA.");
    }

    /** Removes a player from the FFA and returns them to the lobby. */
    public void leaveFfa(Player player) {
        if (player == null) return;

        boolean removed = participants.remove(player.getUniqueId());
        sessionKills.remove(player.getUniqueId());
        sessionKillstreak.remove(player.getUniqueId());

        if (player.isOnline()) {
            player.setWorldBorder(null);
            try {
                player.setHealth(Objects.requireNonNull(
                        player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
            } catch (Exception ignored) {}
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
            plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
        }

        if (removed) plugin.getLogger().info("[FFAManager] " + player.getName() + " left the FFA.");
    }

    // -----------------------------------------------------------------------
    // Kill / respawn
    // -----------------------------------------------------------------------

    public void handleKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;

        UUID killerUuid = killer.getUniqueId();
        int kills = sessionKills.merge(killerUuid, 1, Integer::sum);
        int streak = sessionKillstreak.merge(killerUuid, 1, Integer::sum);

        killer.setHealth(Objects.requireNonNull(
                killer.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        killer.setFoodLevel(20);
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        killer.sendMessage(MM.deserialize("<gray>You killed <red>" + victim.getName()
                + "</red>! <dark_gray>(<green>" + kills + " kills</green>)"));

        if (streak % 5 == 0) {
            broadcast(MM.deserialize("<gold>" + killer.getName() + "</gold> <yellow>is on a <gold>"
                    + streak + "</gold> killstreak!"));
        }

        tryUpdateLemonCoreStats(killer, victim);
    }

    /** Resets the FFA killstreak for a player (called on their death). */
    public void resetKillstreak(UUID uuid) {
        sessionKillstreak.put(uuid, 0);
    }

    /**
     * Restores health/food, re-applies the FFA kit and the zone border. Called
     * from the respawn listener so every FFA death flows through one path.
     */
    public void respawnEquip(Player player) {
        if (player == null) return;
        preparePlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        applyFfaKit(player);
        applyBorder(player);
    }

    /** A safe respawn point inside the current zone (or {@code null} if no zone). */
    public Location getRespawnLocation() {
        if (world() == null || currentCenter == null) return null;
        return scatterLocation();
    }

    private void broadcast(Component message) {
        for (UUID uuid : snapshot()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.sendMessage(message);
        }
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public boolean isInFfa(UUID playerUuid) {
        return participants.contains(playerUuid);
    }

    public int getPlayerCount() {
        return participants.size();
    }

    // -----------------------------------------------------------------------
    // Zone geometry: random center, scatter, border
    // -----------------------------------------------------------------------

    /** A random center near the world spawn, on the surface. */
    private Location randomCenter() {
        World w = world();
        if (w == null) return null;
        Location base = w.getSpawnLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int range = centerRange();
        int x = base.getBlockX() + rng.nextInt(-range, range + 1);
        int z = base.getBlockZ() + rng.nextInt(-range, range + 1);
        int y = w.getHighestBlockYAt(x, z);
        return new Location(w, x + 0.5, y + 1.0, z + 0.5);
    }

    /**
     * A safe surface location inside the current zone (highest non-air block,
     * avoiding lava/water where possible).
     */
    private Location scatterLocation() {
        World w = world();
        if (w == null || currentCenter == null) return null;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double half = Math.max(1.0, size() / 2.0 - 5.0);

        Location fallback = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rng.nextDouble(0, Math.PI * 2);
            double r = half * Math.sqrt(rng.nextDouble());
            int x = (int) Math.round(currentCenter.getX() + r * Math.cos(angle));
            int z = (int) Math.round(currentCenter.getZ() + r * Math.sin(angle));

            Location surface = surfaceAt(x, z);
            if (surface == null) continue;
            Material ground = surface.clone().subtract(0, 1, 0).getBlock().getType();
            if (ground == Material.LAVA) continue;
            if (ground == Material.WATER) {
                if (fallback == null) fallback = surface;
                continue;
            }
            return surface;
        }
        return fallback != null ? fallback : surfaceAt(currentCenter.getBlockX(), currentCenter.getBlockZ());
    }

    private Location surfaceAt(int x, int z) {
        World w = world();
        if (w == null) return null;
        int y = w.getHighestBlockYAt(x, z);
        Block highest = w.getBlockAt(x, y, z);
        if (highest.getType() == Material.AIR) return null;
        return new Location(w, x + 0.5, y + 1.0, z + 0.5);
    }

    /** Applies a fresh per-player border sized to the current zone. */
    private void applyBorder(Player player) {
        if (currentCenter == null) return;
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(currentCenter.getX(), currentCenter.getZ());
        border.setSize(size());
        border.setWarningDistance(borderWarning());
        border.setDamageAmount(0.2);
        border.setDamageBuffer(0.0);
        player.setWorldBorder(border);
    }

    // -----------------------------------------------------------------------
    // Scoreboard / entrance
    // -----------------------------------------------------------------------

    private void renderScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        if (!participants.contains(uuid)) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("ffa", Criteria.DUMMY,
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>Fꜰᴀ</bold></gradient>"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Component> lines = new ArrayList<>();
        lines.add(MM.deserialize("<dark_gray><st>                </st>"));
        lines.add(MM.deserialize("<gray>Kills: <green>" + sessionKills.getOrDefault(uuid, 0)));
        lines.add(MM.deserialize("<gray>Killstreak: <gold>" + sessionKillstreak.getOrDefault(uuid, 0)));
        lines.add(Component.empty());
        lines.add(MM.deserialize("<gray>Players: <white>" + participants.size()));
        lines.add(MM.deserialize("<gray>Zone: <white>" + (int) size() + " blocks"));
        lines.add(MM.deserialize("<gray>Moves in: <yellow>" + formatTime(secondsUntilRelocate)));
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

    private void showFfaEntrance(Player player) {
        player.showTitle(Title.title(
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>Fꜰᴀ</bold></gradient>"),
                MM.deserialize("<gray>Fixed " + (int) size() + "-block zone <dark_gray>|"
                        + " <gray>Players: <green>" + participants.size()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500))));

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline())
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);
        }, 6L);

        Location loc = player.getLocation();
        int count = 24;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = loc.getX() + 1.2 * Math.cos(angle);
            double z = loc.getZ() + 1.2 * Math.sin(angle);
            Location pLoc = new Location(loc.getWorld(), x, loc.getY() + 0.1, z);
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 2, 0, 0, 0, 0,
                    new Particle.DustOptions(i % 2 == 0 ? Color.YELLOW : Color.LIME, 1.2f));
        }

        player.sendMessage(MM.deserialize(
                "<gradient:#fffb00:#00ff00><bold>FFA</bold></gradient> "
                + "<green>You joined the FFA! <dark_gray>(<gray>" + participants.size()
                + " players<dark_gray>) <gray>The zone relocates every <white>"
                + relocateMinutes() + " min<gray>."));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? (m + "m " + s + "s") : (s + "s");
    }

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
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), kitGamemode());
        if (kit == null) {
            plugin.getLogger().warning("[FFAManager] No kit found for gamemode '" + kitGamemode() + "'.");
            return;
        }
        kit.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    private void tryUpdateLemonCoreStats(Player killer, Player victim) {
        try {
            org.bukkit.plugin.Plugin lemonCore = Bukkit.getPluginManager().getPlugin("LemonCore");
            if (lemonCore == null || !lemonCore.isEnabled()) return;
            Class<?> statsClass = Class.forName("com.lemonpvp.lemoncore.stats.StatsAPI");
            java.lang.reflect.Method recordKill = statsClass.getMethod("recordKill", Player.class, Player.class);
            recordKill.invoke(null, killer, victim);
        } catch (Exception ignored) {
            // LemonCore not present or API changed — silently skip.
        }
    }
}
