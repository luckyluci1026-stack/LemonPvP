package com.lemonpvp.lemoncore.lemonlang;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.ast.*;
import com.lemonpvp.lemoncore.lemonlang.runtime.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Routes Bukkit events to matching LemonLang trigger definitions.
 */
public final class LemonLangEventListener implements Listener {

    private static final Logger LOG = Logger.getLogger("LemonLang");

    private final LemonCore plugin;

    public LemonLangEventListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    // ---- Helper ----------------------------------------------------------------

    private LemonLangManager manager() {
        return plugin.getLemonLangManager();
    }

    /** Run all triggers matching the event name for the given player, on main thread. */
    private void fire(String event, Player player, Map<String, String> extraData) {
        LemonLangManager mgr = manager();
        if (mgr == null) return;

        List<TriggerDef> triggers = mgr.getTriggers(event);
        if (triggers.isEmpty()) return;

        Interpreter interp = new Interpreter("event:" + event);

        for (TriggerDef trigger : triggers) {
            Environment childEnv = mgr.getEnvironment().child();
            ScriptContext ctx = new ScriptContext(player, event, childEnv, mgr);
            // Inject extra event data
            if (extraData != null) ctx.data().putAll(extraData);

            try {
                interp.run(trigger.body(), ctx);
            } catch (Exception e) {
                LOG.warning("[LemonLang] Fehler in Trigger '" + event + "': " + e.getMessage());
            }
        }
    }

    /** Fire on main thread (wraps fire() call inside runTask). */
    private void fireOnMain(String event, Player player, Map<String, String> extraData) {
        Bukkit.getScheduler().runTask(plugin, () -> fire(event, player, extraData));
    }

    // ---- Events ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        fireOnMain("join", e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        // Cleanup cooldowns on quit
        LemonLangManager mgr = manager();
        if (mgr != null) mgr.getCooldownRegistry().cleanup(player.getUniqueId());
        fireOnMain("quit", player, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        Map<String, String> data = new HashMap<>();
        if (player.getKiller() != null) {
            data.put("killer", player.getKiller().getName());
        }
        fireOnMain("death", player, data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        fireOnMain("respawn", e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();
        Map<String, String> data = Map.of("message", message);
        // Chat events are async — dispatch to main thread
        Bukkit.getScheduler().runTask(plugin, () -> fire("chat", player, data));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Only fire if block changed to avoid spam
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }
        fire("move", e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player == null) return;

        // General interact trigger
        fireOnMain("interact", player, null);

        // Item-specific rightclick/leftclick triggers
        LemonLangManager mgr = manager();
        if (mgr == null) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir()) return;

        String lemonItemName = mgr.getItemRegistry().getLemonName(handItem, plugin);
        if (lemonItemName == null) return;

        // Determine which action (rightclick or leftclick)
        String triggerEvent = null;
        org.bukkit.event.block.Action action = e.getAction();
        if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
            action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            triggerEvent = "rightclick";
        } else if (action == org.bukkit.event.block.Action.LEFT_CLICK_AIR ||
                   action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            triggerEvent = "leftclick";
        }

        if (triggerEvent == null) return;

        // Find triggers for this specific item
        for (Node node : collectNodes(mgr)) {
            if (!(node instanceof ItemDef item)) continue;
            if (!item.name().equals(lemonItemName)) continue;

            // Check cooldown
            String cooldownKey = "item_" + lemonItemName;
            if (mgr.getCooldownRegistry().isOnCooldown(player.getUniqueId(), cooldownKey)) continue;

            // Apply item cooldown if set
            String cooldownStr = item.props().get("cooldown");
            if (cooldownStr != null && !cooldownStr.isBlank()) {
                try {
                    long ms = com.lemonpvp.lemoncore.lemonlang.token.ArgScanner.parseDuration(cooldownStr.trim(), item.name(), 0);
                    mgr.getCooldownRegistry().setCooldown(player.getUniqueId(), cooldownKey, ms);
                } catch (Exception ignored) {}
            }

            String finalTriggerEvent = triggerEvent;
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (TriggerDef trigger : item.triggers()) {
                    if (!finalTriggerEvent.equalsIgnoreCase(trigger.event())) continue;
                    Environment childEnv = mgr.getEnvironment().child();
                    ScriptContext ctx = new ScriptContext(player, item.name(), childEnv, mgr);
                    Interpreter interp = new Interpreter(item.name());
                    try {
                        interp.run(trigger.body(), ctx);
                    } catch (Exception ex) {
                        java.util.logging.Logger.getLogger("LemonLang").warning(
                                "[LemonLang] Fehler in Item-Trigger '" + item.name() + "': " + ex.getMessage());
                    }
                }
            });
        }
    }

    private java.util.Collection<Node> collectNodes(LemonLangManager mgr) {
        java.util.List<Node> all = new java.util.ArrayList<>();
        for (Program p : mgr.getPrograms().values()) {
            all.addAll(p.nodes());
        }
        return all;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        Map<String, String> data = new HashMap<>();
        data.put("attacker", attacker.getName());
        data.put("victim", victim.getName());
        // Fire for both attacker and victim contexts
        Bukkit.getScheduler().runTask(plugin, () -> {
            fire("hit", attacker, data);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        Map<String, String> data = new HashMap<>();
        data.put("attacker", killer.getName());
        data.put("victim", victim.getName());
        fireOnMain("kill", killer, data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        fireOnMain("blockbreak", e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        fireOnMain("blockplace", e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        LemonLangManager mgr = manager();
        if (mgr == null) return;

        // Check if this is a LemonLang GUI
        var guiDef = mgr.getGuiRegistry().getDefForInventory(e.getInventory());
        if (guiDef == null) return;

        e.setCancelled(true); // Cancel all clicks in LemonLang GUIs

        int slot = e.getSlot();
        SlotDef slotDef = mgr.getGuiRegistry().getSlotDef(guiDef, slot);
        if (slotDef == null || slotDef.onClick() == null) return;

        TriggerDef clickTrigger = slotDef.onClick();
        Environment childEnv = mgr.getEnvironment().child();
        ScriptContext ctx = new ScriptContext(player, guiDef.name(), childEnv, mgr);

        Interpreter interp = new Interpreter(guiDef.name());
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                interp.run(clickTrigger.body(), ctx);
            } catch (Exception ex) {
                LOG.warning("[LemonLang] Fehler in GUI-Click-Trigger '" + guiDef.name() + "' Slot " + slot + ": " + ex.getMessage());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        LemonLangManager mgr = manager();
        if (mgr != null) {
            mgr.getGuiRegistry().removeOpen(e.getInventory());
        }
    }
}
