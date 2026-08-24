package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the lobby feel alive: floating portal signs with live player counts, ambient particles
 * around each portal, and an arrival moment when a player lands in the hub.
 *
 * <p>The portal ring matches the layout {@code SpawnBuilder} builds ("Citadel Aeternum"). Which
 * gamemode a portal advertises is configurable — {@code lobby.portals.<key>} — so the four
 * decorative portals can be pointed at a real queue later without touching code.
 *
 * <p>Everything here is cosmetic and defensive: labels are non-persistent and tagged, so they are
 * never written to the world save and stale ones are swept on start; particle work is skipped
 * entirely when nobody is nearby.
 */
public class LobbyAmbienceManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** One portal on the ring. {@code gamemode} may be blank for a purely decorative arch. */
    private record Portal(String key, String label, String gamemode, int x, int y, int z,
                          Particle particle) {}

    /**
     * Positions come from the SpawnBuilder layout; the surface sits at Y=64, so labels float a
     * little above head height. Four of the eight arches map onto a real gamemode today.
     */
    private static final List<Portal> PORTALS = List.of(
            new Portal("crystal", "<aqua>Crystal",          "crystal",  55, 64,   0, Particle.END_ROD),
            new Portal("sword",   "<yellow>Sword",          "sword",   -55, 64,   0, Particle.CRIT),
            new Portal("mace",    "<gold>Mace",             "mace",      0, 64,  65, Particle.ELECTRIC_SPARK),
            new Portal("shield",  "<blue>Shield",           "shield",  -38, 64,  55, Particle.SOUL_FIRE_FLAME),
            new Portal("bow",     "<green>Bow",             "",          0, 64, -65, Particle.ENCHANT),
            new Portal("totem",   "<light_purple>Totem",    "",         38, 64, -45, Particle.TOTEM_OF_UNDYING),
            new Portal("axe",     "<red>Axe",               "",        -38, 64, -45, Particle.CRIT),
            new Portal("trident", "<dark_aqua>Trident",     "",         38, 64,  55, Particle.NAUTILUS)
    );

    /** A portal the player is standing in, with the gamemode it currently advertises. */
    public record PortalSpot(String key, String gamemode, Particle particle) {}

    private static final double PARTICLE_RANGE = 26.0;
    private static final double PARTICLE_RANGE_SQ = PARTICLE_RANGE * PARTICLE_RANGE;

    private final LemonPractice plugin;
    private final NamespacedKey markerKey;
    private final List<TextDisplay> labels = new ArrayList<>();

    private BukkitTask particleTask;
    private BukkitTask labelTask;

    public LobbyAmbienceManager(LemonPractice plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "lobby_ambience");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("lobby.ambience.enabled", true);
    }

    /** Resolves the lobby world, falling back to the first loaded world. */
    private World lobbyWorld() {
        String name = plugin.getConfig().getString("lobby.world", "world");
        World w = Bukkit.getWorld(name);
        if (w != null) return w;
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    public void start() {
        if (!isEnabled()) return;
        World world = lobbyWorld();
        if (world == null) {
            plugin.getLogger().warning("[LobbyAmbience] No world to decorate — skipping.");
            return;
        }

        sweepStaleLabels(world);
        spawnLabels(world);

        // Refresh the live counts a couple of times a second's worth of ticks apart; the text is
        // cheap to rebuild and this keeps the numbers feeling immediate without spamming packets.
        labelTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshLabels, 40L, 40L);
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::emitParticles, 20L, 10L);

        plugin.getLogger().info("[LobbyAmbience] Lobby ambience active (" + labels.size() + " portal signs).");
    }

    public void shutdown() {
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }
        if (labelTask != null)    { labelTask.cancel();    labelTask = null; }
        for (TextDisplay d : labels) {
            if (d != null && d.isValid()) d.remove();
        }
        labels.clear();
    }

    /**
     * Removes labels left behind by a previous run. They are spawned non-persistent so a clean
     * shutdown leaves nothing, but a crash (or /reload) can, and duplicates would stack up.
     */
    private void sweepStaleLabels(World world) {
        int removed = 0;
        for (TextDisplay d : world.getEntitiesByClass(TextDisplay.class)) {
            if (d.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) {
                d.remove();
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("[LobbyAmbience] Cleared " + removed + " leftover portal sign(s).");
        }
    }

    // ── Portal signs ────────────────────────────────────────────────────────

    private void spawnLabels(World world) {
        for (Portal p : PORTALS) {
            Location loc = new Location(world, p.x() + 0.5, p.y() + 3.4, p.z() + 0.5);
            TextDisplay disp = world.spawn(loc, TextDisplay.class, d -> {
                d.text(labelText(p));
                d.setBillboard(Display.Billboard.CENTER);        // always faces the viewer
                d.setSeeThrough(false);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // no dark plate behind the text
                d.setViewRange(1.2f);
                d.setPersistent(false);                           // never written to the world save
                d.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            });
            labels.add(disp);
        }
    }

    private void refreshLabels() {
        for (int i = 0; i < labels.size() && i < PORTALS.size(); i++) {
            TextDisplay d = labels.get(i);
            if (d != null && d.isValid()) d.text(labelText(PORTALS.get(i)));
        }
    }

    /** Portal name plus, when it advertises a real gamemode, the live counts and the walk-in hint. */
    private Component labelText(Portal p) {
        String gamemode = gamemodeOf(p);
        StringBuilder sb = new StringBuilder("<bold>").append(p.label()).append("</bold>");
        if (gamemode != null && !gamemode.isBlank()
                && plugin.getGamemodeManager() != null
                && plugin.getGamemodeManager().exists(gamemode)
                && plugin.getQueueManager() != null) {
            int playing = plugin.getQueueManager().getPlayingCount(gamemode);
            int queued  = plugin.getQueueManager().getQueueCount(gamemode);
            sb.append("\n<gray>").append(playing).append(" playing");
            if (queued > 0) sb.append(" <dark_gray>·<gray> ").append(queued).append(" in queue");
            // Only promise this where walking in actually queues you.
            if (plugin.getConfig().getBoolean("lobby.portals.walk-in", true)) {
                sb.append("\n<dark_gray>▸ <white>Walk in to queue");
            }
        }
        return MM.deserialize(sb.toString());
    }

    // ── Lookup ──────────────────────────────────────────────────────────────

    /** The gamemode a portal advertises: config override first, then the built-in default. */
    private String gamemodeOf(Portal p) {
        return plugin.getConfig().getString("lobby.portals." + p.key(), p.gamemode());
    }

    /**
     * The portal a location is standing in, or null. Shared with the walk-in listener so both it
     * and the signs read one portal table.
     */
    public PortalSpot portalAt(Location loc, double radiusSq, double maxYDelta) {
        if (loc == null || loc.getWorld() == null) return null;
        World lobby = lobbyWorld();
        if (lobby == null || !lobby.equals(loc.getWorld())) return null;

        for (Portal p : PORTALS) {
            if (Math.abs(loc.getY() - p.y()) > maxYDelta) continue;
            double dx = loc.getX() - (p.x() + 0.5);
            double dz = loc.getZ() - (p.z() + 0.5);
            if (dx * dx + dz * dz <= radiusSq) {
                return new PortalSpot(p.key(), gamemodeOf(p), p.particle());
            }
        }
        return null;
    }

    // ── Ambience ────────────────────────────────────────────────────────────

    /** Emits a little motion at each portal, but only where someone is close enough to see it. */
    private void emitParticles() {
        World world = lobbyWorld();
        if (world == null) return;
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        for (Portal p : PORTALS) {
            double px = p.x() + 0.5, py = p.y() + 1.2, pz = p.z() + 0.5;
            if (!anyoneNear(players, px, py, pz)) continue;
            world.spawnParticle(p.particle(), px, py, pz, 6, 0.6, 0.9, 0.6, 0.02);
        }

        // A slow shimmer over the central plaza fountains so the middle of the hub isn't static.
        if (anyoneNear(players, 0.5, 66.0, 0.5)) {
            world.spawnParticle(Particle.END_ROD, 0.5, 66.0, 0.5, 4, 1.4, 0.8, 1.4, 0.01);
        }
    }

    private boolean anyoneNear(List<Player> players, double x, double y, double z) {
        for (Player pl : players) {
            Location l = pl.getLocation();
            double dx = l.getX() - x, dy = l.getY() - y, dz = l.getZ() - z;
            if (dx * dx + dy * dy + dz * dz <= PARTICLE_RANGE_SQ) return true;
        }
        return false;
    }

    // ── Arrival ─────────────────────────────────────────────────────────────

    /** The moment a player lands in the hub. Called from the lobby join handler. */
    public void welcome(Player player) {
        if (!isEnabled()) return;
        if (!plugin.getConfig().getBoolean("lobby.ambience.welcome-title", true)) return;

        player.showTitle(Title.title(
                MM.deserialize("<gradient:#fffb00:#00ff00><bold>LᴇᴍᴏɴPᴠP</bold></gradient>"),
                MM.deserialize("<gray>Welcome back, <white>" + player.getName() + "<gray>."),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2200),
                        Duration.ofMillis(700))));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.45f, 1.6f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
    }
}
