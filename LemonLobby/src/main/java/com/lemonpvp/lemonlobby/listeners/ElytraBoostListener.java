package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hub elytra boost.
 *
 * <p>Lobby players automatically wear a (locked, unbreakable) elytra. Combined
 * with the double jump they can launch up, open the elytra mid-air and then
 * right-click while gliding for an explosive speed boost — an explosion-particle
 * burst, a bang, and a velocity push in the look direction, on a short cooldown.</p>
 *
 * <p>Fall and fly-into-wall damage are cancelled so crashes never hurt. The
 * elytra is PDC-marked: it can't be moved, dropped, or unequipped, and it is
 * never given on other servers (duel/FFA prep clears armor anyway).</p>
 *
 * <p>Tunable via config: {@code elytra-boost.enabled}, {@code elytra-boost.power},
 * {@code elytra-boost.cooldown-ms}, {@code elytra-boost.give-elytra}.</p>
 */
public class ElytraBoostListener implements Listener {

    private final LemonLobby plugin;
    private final NamespacedKey elytraKey;

    /** uuid -> epoch-ms of the last boost (cooldown tracking). */
    private final Map<UUID, Long> lastBoost = new ConcurrentHashMap<>();

    public ElytraBoostListener(LemonLobby plugin) {
        this.plugin = plugin;
        this.elytraKey = new NamespacedKey(plugin, "lobby_elytra");
        // Inventory-clearing flows (kit editor, admin clears) silently remove
        // the wings — re-equip every 5s for anyone whose chest slot is empty.
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::reequipAll, 100L, 100L);
    }

    private void reequipAll() {
        if (!enabled() || !plugin.getConfig().getBoolean("elytra-boost.give-elytra", true)) return;
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            GameMode gm = p.getGameMode();
            if (gm != GameMode.ADVENTURE && gm != GameMode.SURVIVAL) continue;
            ItemStack chest = p.getInventory().getChestplate();
            if (chest == null || chest.getType() == Material.AIR) {
                p.getInventory().setChestplate(buildElytra());
            }
        }
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("elytra-boost.enabled", true);
    }

    // -------------------------------------------------------------------------
    // Equip on join
    // -------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("elytra-boost.give-elytra", true)) return;
        Player p = event.getPlayer();
        GameMode gm = p.getGameMode();
        if (gm != GameMode.ADVENTURE && gm != GameMode.SURVIVAL) return;

        ItemStack chest = p.getInventory().getChestplate();
        if (chest != null && chest.getType() != Material.AIR && !isLobbyElytra(chest)) return; // keep real armor
        p.getInventory().setChestplate(buildElytra());
    }

    private ItemStack buildElytra() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<!italic><gradient:#fffb00:#00ff00>Lobby Wings</gradient>"));
            meta.lore(java.util.List.of(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<!italic><gray>Glide and right-click to boost!")));
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(elytraKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isLobbyElytra(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(elytraKey, PersistentDataType.BYTE);
    }

    // -------------------------------------------------------------------------
    // Boost — right-click while gliding
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        if (!p.isGliding()) return;

        long cooldown = plugin.getConfig().getLong("elytra-boost.cooldown-ms", 1500);
        long now = System.currentTimeMillis();
        Long last = lastBoost.get(p.getUniqueId());
        if (last != null && now - last < cooldown) {
            double remaining = (cooldown - (now - last)) / 1000.0;
            p.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<!italic><gray>Boost ready in <yellow>"
                            + String.format(java.util.Locale.US, "%.1f", remaining) + "s"));
            return;
        }
        lastBoost.put(p.getUniqueId(), now);

        double power = plugin.getConfig().getDouble("elytra-boost.power", 1.6);
        Vector dir = p.getLocation().getDirection().normalize().multiply(power);
        p.setVelocity(p.getVelocity().add(dir));

        // Explosion burst behind the player + bang.
        p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 2, 0.2, 0.2, 0.2, 0);
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 20, 0.25, 0.25, 0.25, 0.06);
        p.getWorld().spawnParticle(Particle.SMOKE, p.getLocation(), 12, 0.2, 0.2, 0.2, 0.04);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.1f);
    }

    // -------------------------------------------------------------------------
    // Protect the lobby elytra + gliding players
    // -------------------------------------------------------------------------

    /** The lobby elytra can't be moved out of the chest slot. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (isLobbyElytra(current)) event.setCancelled(true);
    }

    /** ...and can't be dropped. */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isLobbyElytra(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    /** Crashing into the ground or a wall never hurts in the lobby. */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastBoost.remove(event.getPlayer().getUniqueId());
    }
}
