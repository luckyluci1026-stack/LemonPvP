package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.cape.CapeImage;
import com.lemonpvp.lemoncosmetics.cape.CapeRenderer;
import com.lemonpvp.lemoncosmetics.cape.MapCosmeticSlot;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Renders custom, optionally-animated map-cosmetics on players WITHOUT a
 * resource pack — currently capes (back) and bandanas (head), one per
 * {@link MapCosmeticSlot}.
 *
 * <p>Pipeline: an uploaded image ({@code plugins/LemonCosmetics/<slot>/*.png|gif})
 * is decoded to 128x128 frames ({@link CapeImage}), drawn onto a vanilla map
 * ({@link CapeRenderer}), placed on an {@link ItemDisplay} holding that filled
 * map, and mounted as a passenger of the player so it follows smoothly (client
 * interpolated — no per-tick teleport, no lag). Placement is config-driven under
 * {@code <slot>.*}. A lightweight refresh loop re-mounts displays that get
 * detached (e.g. after a teleport), mirroring LemonNameTags.</p>
 */
public class CapeManager {

    private final LemonCosmetics plugin;
    private final Map<UUID, Map<MapCosmeticSlot, Active>> active = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    private record Active(UUID displayId, MapView map, CapeRenderer renderer,
                          BukkitTask task, String fileName) {}

    public CapeManager(LemonCosmetics plugin) {
        this.plugin = plugin;
        for (MapCosmeticSlot slot : MapCosmeticSlot.values()) {
            File dir = folder(slot);
            if (!dir.exists() && !dir.mkdirs()) {
                plugin.getLogger().warning("[CapeManager] Could not create directory " + dir);
            }
        }
        startRefreshLoop();
    }

    private File folder(MapCosmeticSlot slot) {
        return new File(plugin.getDataFolder(), slot.folder);
    }

    /**
     * Vanilla only streams map pixels for maps held in hands/item frames — NOT
     * for maps inside display entities. Without an explicit push, viewers see
     * blank paper. So we send the map data ourselves to every player near the
     * wearer (this is why no resource pack is needed).
     */
    private static final double MAP_SEND_RANGE_SQ = 48 * 48;

    private void broadcastMap(Player wearer, MapView map) {
        for (Player p : wearer.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(wearer.getLocation()) <= MAP_SEND_RANGE_SQ) {
                p.sendMap(map);
            }
        }
    }

    /**
     * Re-mounts any display that got detached from its player (e.g. on teleport)
     * and re-broadcasts map data so players who just came into range see the
     * texture (vanilla never sends it for display entities).
     */
    private void startRefreshLoop() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Map<MapCosmeticSlot, Active>> e : active.entrySet()) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p == null || !p.isOnline()) continue;
                for (Active a : e.getValue().values()) {
                    Entity disp = Bukkit.getEntity(a.displayId());
                    if (disp != null && !p.getPassengers().contains(disp)) {
                        p.addPassenger(disp);
                    }
                    broadcastMap(p, a.map());
                }
            }
        }, 20L, 20L);
    }

    /** Base names of all uploaded files for a slot. */
    public List<String> list(MapCosmeticSlot slot) {
        File[] files = folder(slot).listFiles((dir, n) -> {
            String l = n.toLowerCase(Locale.ROOT);
            return l.endsWith(".png") || l.endsWith(".gif") || l.endsWith(".jpg") || l.endsWith(".jpeg");
        });
        if (files == null) return List.of();
        return java.util.Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
    }

    private File resolve(MapCosmeticSlot slot, String name) {
        File dir = folder(slot);
        File direct = new File(dir, name);
        if (direct.isFile()) return direct;
        for (String ext : new String[]{".png", ".gif", ".jpg", ".jpeg"}) {
            File f = new File(dir, name + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    /**
     * Equips a map-cosmetic in the given slot. Returns false if the file is
     * missing or undecodable. Any cosmetic already in that slot is removed first.
     */
    public boolean equip(Player player, MapCosmeticSlot slot, String name) {
        File file = resolve(slot, name);
        if (file == null) return false;

        CapeImage image;
        try {
            image = CapeImage.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("[CapeManager] Failed to load " + slot + " '" + name + "': " + e.getMessage());
            return false;
        }

        unequip(player.getUniqueId(), slot);

        MapView map = Bukkit.createMap(player.getWorld());
        for (MapRenderer r : new ArrayList<>(map.getRenderers())) map.removeRenderer(r);
        CapeRenderer renderer = new CapeRenderer(image);
        map.addRenderer(renderer);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        if (mapItem.getItemMeta() instanceof MapMeta mm) {
            mm.setMapView(map);
            mapItem.setItemMeta(mm);
        }

        final Transformation transform = buildTransform(slot);
        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(mapItem);
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setTransformation(transform);
            d.setInterpolationDuration(0);
            d.setViewRange(cfgFloat(slot, "view-range", 2.0f));
            d.setBrightness(new Display.Brightness(15, 15));
        });

        if (!player.addPassenger(display)) {
            plugin.getLogger().warning("[CapeManager] Could not mount " + slot + " on " + player.getName());
            display.remove();
            return false;
        }

        // Push the first frame's pixels to everyone nearby right away.
        broadcastMap(player, map);

        final int frames = renderer.frameCount();
        BukkitTask task = null;
        if (frames > 1) {
            final int period = Math.max(1, (int) cfgFloat(slot, "frame-ticks", slot.frameTicks));
            final int[] f = {0};
            final UUID uuid = player.getUniqueId();
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Map<MapCosmeticSlot, Active> slots = active.get(uuid);
                if (slots == null || !slots.containsKey(slot)) return;
                renderer.setFrame(f[0]++);
                // Stream the new frame to nearby viewers (vanilla won't).
                Player wearer = Bukkit.getPlayer(uuid);
                if (wearer != null && wearer.isOnline()) broadcastMap(wearer, map);
            }, period, period);
        }

        active.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(MapCosmeticSlot.class))
                .put(slot, new Active(display.getUniqueId(), map, renderer, task, file.getName()));
        return true;
    }

    /** Removes the cosmetic in one slot. */
    public void unequip(UUID uuid, MapCosmeticSlot slot) {
        Map<MapCosmeticSlot, Active> slots = active.get(uuid);
        if (slots == null) return;
        Active a = slots.remove(slot);
        if (slots.isEmpty()) active.remove(uuid);
        removeActive(a);
    }

    /** Removes every map-cosmetic for the player (e.g. on quit). */
    public void unequipAll(UUID uuid) {
        Map<MapCosmeticSlot, Active> slots = active.remove(uuid);
        if (slots == null) return;
        for (Active a : slots.values()) removeActive(a);
    }

    private void removeActive(Active a) {
        if (a == null) return;
        if (a.task() != null) a.task().cancel();
        Entity e = Bukkit.getEntity(a.displayId());
        if (e != null) e.remove();
    }

    /** File currently worn in the slot, or null. */
    public String current(UUID uuid, MapCosmeticSlot slot) {
        Map<MapCosmeticSlot, Active> slots = active.get(uuid);
        Active a = slots != null ? slots.get(slot) : null;
        return a != null ? a.fileName() : null;
    }

    /** Cancels the refresh loop and removes all cosmetics (plugin disable). */
    public void shutdown() {
        if (refreshTask != null) refreshTask.cancel();
        for (UUID uuid : new ArrayList<>(active.keySet())) unequipAll(uuid);
    }

    // ------------------------------------------------------------------
    // Placement transform (slot defaults, overridable in config)
    // ------------------------------------------------------------------

    private Transformation buildTransform(MapCosmeticSlot slot) {
        Vector3f translation = new Vector3f(
                cfgFloat(slot, "offset-x", slot.offX),
                cfgFloat(slot, "offset-y", slot.offY),
                cfgFloat(slot, "offset-z", slot.offZ));
        Vector3f scale = new Vector3f(
                cfgFloat(slot, "scale-x", slot.scaleX),
                cfgFloat(slot, "scale-y", slot.scaleY),
                cfgFloat(slot, "scale-z", slot.scaleZ));
        Quaternionf leftRotation = new Quaternionf()
                .rotateY((float) Math.toRadians(cfgFloat(slot, "yaw-offset", slot.yaw)))
                .rotateX((float) Math.toRadians(cfgFloat(slot, "pitch", slot.pitch)));
        return new Transformation(translation, leftRotation, scale, new Quaternionf());
    }

    private float cfgFloat(MapCosmeticSlot slot, String key, float def) {
        return (float) plugin.getConfig().getDouble(slot.configPrefix + "." + key, def);
    }
}
