package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.cape.CapeImage;
import com.lemonpvp.lemoncosmetics.cape.CapeRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Plays "emotes" — animated images shown above a player's head — WITHOUT a
 * resource pack, using the same map-on-ItemDisplay engine as capes/bandanas.
 *
 * <p>Creating an emote is just dropping a {@code .gif} (or static png) into
 * {@code plugins/LemonCosmetics/emotes/}. {@code /emote <name>} plays it once
 * through (gifs) or for a fixed hold time (static images), then the display
 * removes itself. The display is mounted as a passenger (smooth follow) and
 * billboarded so every viewer sees it face-on.</p>
 */
public class EmoteManager {

    private final LemonCosmetics plugin;
    private final File emoteDir;
    /** One active emote per player; replaced if a new one is played. */
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();
    /** Simple per-player cooldown (epoch ms of next allowed use). */
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    private record Active(UUID displayId, BukkitTask task) {}

    /** Default emote pack bundled in the jar; extracted on first run. */
    private static final String[] DEFAULT_EMOTES = {
            "gg.gif", "ez.gif", "lol.gif", "nice.gif", "rip.gif", "100.gif",
            "laugh.gif", "cry.gif", "rage.gif", "wink.gif", "smile.gif",
            "skull.gif", "heart.gif", "fire.gif", "clown.gif"
    };

    public EmoteManager(LemonCosmetics plugin) {
        this.plugin = plugin;
        this.emoteDir = new File(plugin.getDataFolder(), "emotes");
        if (!emoteDir.exists() && !emoteDir.mkdirs()) {
            plugin.getLogger().warning("[EmoteManager] Could not create emotes directory.");
        }
        extractDefaults();
    }

    /** Copies each bundled default emote that isn't already on disk. */
    private void extractDefaults() {
        for (String name : DEFAULT_EMOTES) {
            if (new File(emoteDir, name).isFile()) continue;
            try {
                plugin.saveResource("emotes/" + name, false);
            } catch (IllegalArgumentException e) {
                // resource missing from the jar — skip quietly
            }
        }
    }

    public List<String> listEmotes() {
        File[] files = emoteDir.listFiles((dir, n) -> {
            String l = n.toLowerCase(Locale.ROOT);
            return l.endsWith(".png") || l.endsWith(".gif") || l.endsWith(".jpg") || l.endsWith(".jpeg");
        });
        if (files == null) return List.of();
        return java.util.Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
    }

    private File resolve(String name) {
        File direct = new File(emoteDir, name);
        if (direct.isFile()) return direct;
        for (String ext : new String[]{".gif", ".png", ".jpg", ".jpeg"}) {
            File f = new File(emoteDir, name + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    /** Range within which viewers receive the emote's map pixels. */
    private static final double MAP_SEND_RANGE_SQ = 48 * 48;

    private void broadcastMap(Player wearer, MapView map) {
        for (Player p : wearer.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(wearer.getLocation()) <= MAP_SEND_RANGE_SQ) {
                p.sendMap(map);
            }
        }
    }

    /** Remaining cooldown in seconds, or 0 when ready. */
    public long cooldownLeft(UUID uuid) {
        long until = cooldown.getOrDefault(uuid, 0L);
        return Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    /**
     * Plays an emote above the player's head. Returns false if the file is
     * missing/unreadable. Replaces any emote currently playing for the player.
     */
    public boolean play(Player player, String name) {
        File file = resolve(name);
        if (file == null) return false;

        CapeImage image;
        try {
            image = CapeImage.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("[EmoteManager] Failed to load emote '" + name + "': " + e.getMessage());
            return false;
        }

        stop(player.getUniqueId());
        cooldown.put(player.getUniqueId(),
                System.currentTimeMillis() + 1000L * plugin.getConfig().getInt("emotes.cooldown-seconds", 5));

        MapView map = Bukkit.createMap(player.getWorld());
        for (MapRenderer r : new ArrayList<>(map.getRenderers())) map.removeRenderer(r);
        CapeRenderer renderer = new CapeRenderer(image);
        map.addRenderer(renderer);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        if (mapItem.getItemMeta() instanceof MapMeta mm) {
            mm.setMapView(map);
            mapItem.setItemMeta(mm);
        }

        float size = cfgFloat("scale", 1.0f);
        Transformation transform = new Transformation(
                new Vector3f(0f, cfgFloat("offset-y", 2.45f), 0f),
                new Quaternionf(),
                new Vector3f(size, size, 0.02f),
                new Quaternionf());

        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(mapItem);
            d.setBillboard(Display.Billboard.CENTER); // face every viewer
            d.setPersistent(false);
            d.setTransformation(transform);
            d.setViewRange(cfgFloat("view-range", 2.0f));
            d.setBrightness(new Display.Brightness(15, 15));
        });

        if (!player.addPassenger(display)) {
            display.remove();
            return false;
        }

        // Vanilla only streams map pixels for maps in hands/item frames — NOT
        // display entities. Push the pixels ourselves or viewers see blank paper.
        broadcastMap(player, map);

        // Frame ticker + lifetime: gifs play once through (min 2s, max 10s),
        // static images hold for a fixed time.
        final int frameTicks = Math.max(1, plugin.getConfig().getInt("emotes.frame-ticks", 2));
        final int frames = renderer.frameCount();
        final long lifeTicks = frames > 1
                ? Math.min(200L, Math.max(40L, (long) frames * frameTicks))
                : Math.max(20L, 20L * plugin.getConfig().getInt("emotes.static-seconds", 3));

        final UUID uuid = player.getUniqueId();
        final int[] f = {0};
        final long[] elapsed = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            elapsed[0] += frameTicks;
            if (elapsed[0] >= lifeTicks) {
                stop(uuid);
                return;
            }
            if (frames > 1) {
                renderer.setFrame(f[0]++);
                Player wearer = Bukkit.getPlayer(uuid);
                if (wearer != null && wearer.isOnline()) broadcastMap(wearer, map);
            }
        }, frameTicks, frameTicks);

        active.put(uuid, new Active(display.getUniqueId(), task));
        return true;
    }

    /** Stops the player's active emote, if any. */
    public void stop(UUID uuid) {
        Active a = active.remove(uuid);
        if (a == null) return;
        if (a.task() != null) a.task().cancel();
        Entity e = Bukkit.getEntity(a.displayId());
        if (e != null) e.remove();
    }

    /** Stops everything (plugin disable). */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(active.keySet())) stop(uuid);
    }

    private float cfgFloat(String key, float def) {
        return (float) plugin.getConfig().getDouble("emotes." + key, def);
    }
}
