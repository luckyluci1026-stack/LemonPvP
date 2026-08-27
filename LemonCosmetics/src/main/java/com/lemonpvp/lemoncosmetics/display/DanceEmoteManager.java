package com.lemonpvp.lemoncosmetics.display;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the Fortnite-style dance emotes: the dancer is frozen and made invisible
 * (potion, no particles) while a packet armor-stand body double — wearing their
 * own head and outfit — performs the {@link DanceEmote} keyframes in their
 * place, with a light note-block beat. Everything is packets; no mods, no
 * resource pack, no server entity.
 *
 * <p>A dance ends when its duration elapses, the player runs {@code /emote stop},
 * they take damage, teleport away, or quit — {@link #stop(UUID)} always restores
 * walk speed and removes the invisibility.</p>
 */
public class DanceEmoteManager implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final double VIEW_RANGE_SQ = 48.0 * 48.0;
    private static final int FRAME_TICKS = 2;

    private record ActiveDance(DanceEmote emote, DanceSession session, Location base,
                               float prevWalkSpeed, BukkitTask task) {}

    private final org.bukkit.plugin.Plugin plugin;
    private final Map<UUID, ActiveDance> dancing = new ConcurrentHashMap<>();

    public DanceEmoteManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isDance(String name) {
        return DanceEmote.byId(name) != null;
    }

    public List<String> danceIds() {
        return java.util.Arrays.stream(DanceEmote.values()).map(DanceEmote::id).toList();
    }

    /** Starts a dance for the player. Returns false if the name is no dance. */
    public boolean play(Player dancer, String name) {
        DanceEmote emote = DanceEmote.byId(name);
        if (emote == null) return false;
        stop(dancer.getUniqueId());

        Location base = dancer.getLocation().clone();
        DanceSession session = new DanceSession(
                ownHead(dancer),
                pick(dancer.getInventory().getChestplate(), Material.LEATHER_CHESTPLATE),
                pick(dancer.getInventory().getLeggings(), Material.LEATHER_LEGGINGS),
                pick(dancer.getInventory().getBoots(), Material.LEATHER_BOOTS));

        // Freeze + hide the real body; the double takes their place.
        float prevSpeed = dancer.getWalkSpeed();
        dancer.setWalkSpeed(0f);
        dancer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                emote.durationTicks() + 40, 0, true, false));

        // Spawn the double for everyone nearby — including the dancer (F5 view).
        for (Player viewer : dancer.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(base) <= VIEW_RANGE_SQ) {
                session.spawn(viewer, base);
            }
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= emote.durationTicks() || !dancer.isOnline()) {
                    stop(dancer.getUniqueId());
                    return;
                }
                session.pose(emote.poseAt(t), base);
                if (t % 10 == 0) { // simple two-note beat underneath the dance
                    float pitch = (t / 10) % 2 == 0 ? 1.2f : 0.9f;
                    base.getWorld().playSound(base, Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, pitch);
                }
                t += FRAME_TICKS;
            }
        }, 1L, FRAME_TICKS);

        dancing.put(dancer.getUniqueId(), new ActiveDance(emote, session, base, prevSpeed, task));
        dancer.sendMessage(MM.deserialize("<!italic><gradient:#ffe259:#ffa751><bold>Emote</bold></gradient> "
                + "<dark_gray>» <green>Dancing <yellow>" + emote.id() + "<green>! <gray>(/emote stop to end early)"));
        return true;
    }

    /** Ends a dance (if any) and restores the player. Safe to call twice. */
    public void stop(UUID uuid) {
        ActiveDance d = dancing.remove(uuid);
        if (d == null) return;
        d.task().cancel();
        d.session().destroy();
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.setWalkSpeed(d.prevWalkSpeed() > 0 ? d.prevWalkSpeed() : 0.2f);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    public boolean isDancing(UUID uuid) {
        return dancing.containsKey(uuid);
    }

    /** Ends every running dance (plugin disable) so nobody stays frozen/invisible. */
    public void stopAll() {
        for (UUID uuid : dancing.keySet().toArray(new UUID[0])) stop(uuid);
    }

    // -- Auto-cancel: the dance can't survive these -------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        stop(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) stop(p.getUniqueId());
    }

    // -- Outfit helpers ------------------------------------------------------

    /** The dancer's own head, so the double wears their skin. */
    private ItemStack ownHead(Player dancer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            meta.setPlayerProfile(dancer.getPlayerProfile());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    /** The dancer's real armor piece, or a lemon-yellow leather fallback so the double has a body. */
    private ItemStack pick(ItemStack worn, Material fallback) {
        if (worn != null && !worn.getType().isAir()) return worn.clone();
        ItemStack item = new ItemStack(fallback);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(0xFF, 0xE2, 0x59)); // lemon
            item.setItemMeta(meta);
        }
        return item;
    }
}
