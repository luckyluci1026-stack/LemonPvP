package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.cape.CapeImage;
import com.lemonpvp.lemoncosmetics.cape.CapeRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Renders custom, optionally-animated "capes" on players WITHOUT a resource pack.
 *
 * <p>Pipeline: an uploaded image ({@code plugins/LemonCosmetics/capes/*.png|gif})
 * is decoded to 128x128 frames ({@link CapeImage}), drawn onto a vanilla map
 * ({@link CapeRenderer}), placed on a {@link ItemDisplay} holding that filled
 * map, and mounted as a passenger on the player so it follows smoothly (client
 * interpolated — no per-tick teleport, no lag). Placement (offset/scale/rotation)
 * is fully config-driven under {@code capes:} so it can be tuned live without a
 * code change.</p>
 */
public class CapeManager {

    private final LemonCosmetics plugin;
    private final File capeDir;
    private final java.util.Map<UUID, Active> active = new ConcurrentHashMap<>();

    private record Active(UUID displayId, MapView map, CapeRenderer renderer,
                          BukkitTask task, String capeName) {}

    public CapeManager(LemonCosmetics plugin) {
        this.plugin = plugin;
        this.capeDir = new File(plugin.getDataFolder(), "capes");
        if (!capeDir.exists() && !capeDir.mkdirs()) {
            plugin.getLogger().warning("[CapeManager] Could not create capes directory.");
        }
    }

    /** Lists the base names (without extension) of all uploaded cape images. */
    public List<String> listCapes() {
        File[] files = capeDir.listFiles((dir, n) -> {
            String l = n.toLowerCase(Locale.ROOT);
            return l.endsWith(".png") || l.endsWith(".gif") || l.endsWith(".jpg") || l.endsWith(".jpeg");
        });
        if (files == null) return List.of();
        return java.util.Arrays.stream(files)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /** Resolves a cape file by name, trying common extensions. */
    private File resolve(String name) {
        File direct = new File(capeDir, name);
        if (direct.isFile()) return direct;
        for (String ext : new String[]{".png", ".gif", ".jpg", ".jpeg"}) {
            File f = new File(capeDir, name + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    /**
     * Equips a cape for the player. Returns false if the file is missing or the
     * image can't be decoded. Any previously-worn cape is removed first.
     */
    public boolean equip(Player player, String name) {
        File file = resolve(name);
        if (file == null) return false;

        CapeImage image;
        try {
            image = CapeImage.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("[CapeManager] Failed to load cape '" + name + "': " + e.getMessage());
            return false;
        }

        unequip(player.getUniqueId());

        // Fresh map dedicated to this cape, with only our renderer.
        MapView map = Bukkit.createMap(player.getWorld());
        for (MapRenderer r : new ArrayList<>(map.getRenderers())) map.removeRenderer(r);
        CapeRenderer renderer = new CapeRenderer(image);
        map.addRenderer(renderer);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        if (mapItem.getItemMeta() instanceof MapMeta mm) {
            mm.setMapView(map);
            mapItem.setItemMeta(mm);
        }

        final Transformation transform = buildTransform();
        Location spawn = player.getLocation();
        ItemDisplay display = player.getWorld().spawn(spawn, ItemDisplay.class, d -> {
            d.setItemStack(mapItem);
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setTransformation(transform);
            d.setInterpolationDuration(0);
            d.setViewRange(cfgFloat("view-range", 2.0f));
            d.setBrightness(new Display.Brightness(15, 15)); // full-lit so night capes stay visible
        });

        // Mount as a passenger so it follows the player smoothly, client-side.
        boolean mounted = player.addPassenger(display);
        if (!mounted) {
            plugin.getLogger().warning("[CapeManager] Could not mount cape display on " + player.getName());
            display.remove();
            return false;
        }

        // Animation ticker (frame advance). Static capes (1 frame) do nothing.
        final int frames = renderer.frameCount();
        final int period = Math.max(1, plugin.getConfig().getInt("capes.frame-ticks", 2));
        BukkitTask task;
        if (frames > 1) {
            final int[] f = {0};
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Active a = active.get(player.getUniqueId());
                if (a == null) return;
                renderer.setFrame(f[0]++);
            }, period, period);
        } else {
            task = null;
        }

        active.put(player.getUniqueId(), new Active(display.getUniqueId(), map, renderer, task, file.getName()));
        return true;
    }

    /** Removes the player's cape (display entity + animation task), if any. */
    public void unequip(UUID uuid) {
        Active a = active.remove(uuid);
        if (a == null) return;
        if (a.task() != null) a.task().cancel();
        Entity e = Bukkit.getEntity(a.displayId());
        if (e != null) e.remove();
    }

    /** Name of the cape the player is currently wearing, or null. */
    public String currentCape(UUID uuid) {
        Active a = active.get(uuid);
        return a != null ? a.capeName() : null;
    }

    /** Removes every active cape (plugin disable). */
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(active.keySet())) unequip(uuid);
    }

    // ------------------------------------------------------------------
    // Placement transform (all values live in config for visual tuning)
    // ------------------------------------------------------------------

    private Transformation buildTransform() {
        Vector3f translation = new Vector3f(
                cfgFloat("offset-x", 0.0f),
                cfgFloat("offset-y", 0.2f),
                cfgFloat("offset-z", -0.30f)); // behind the player's back
        Vector3f scale = new Vector3f(
                cfgFloat("scale-x", 0.90f),
                cfgFloat("scale-y", 1.40f),
                cfgFloat("scale-z", 0.02f)); // thin panel
        // Stand the flat map upright, then apply a yaw offset if needed.
        Quaternionf leftRotation = new Quaternionf()
                .rotateY((float) Math.toRadians(cfgFloat("yaw-offset", 180.0f)))
                .rotateX((float) Math.toRadians(cfgFloat("pitch", 90.0f)));
        Quaternionf rightRotation = new Quaternionf();
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }

    private float cfgFloat(String key, float def) {
        return (float) plugin.getConfig().getDouble("capes." + key, def);
    }
}
