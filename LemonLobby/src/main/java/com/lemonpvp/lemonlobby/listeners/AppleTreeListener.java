package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.model.PlankTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

public class AppleTreeListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonLobby plugin;
    private final Map<UUID, Long> appleCooldowns = new HashMap<>();
    private final Map<UUID, Long> plankCooldowns = new HashMap<>();

    private Set<Material> leafMaterials;
    private Set<Material> logMaterials;
    private long appleCooldownMs;
    private long plankCooldownMs;

    public AppleTreeListener(LemonLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        leafMaterials = new HashSet<>();
        logMaterials = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("apple-tree.leaf-materials")) {
            try { leafMaterials.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : plugin.getConfig().getStringList("apple-tree.log-materials")) {
            try { logMaterials.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        appleCooldownMs = plugin.getConfig().getLong("apple-tree.apple-cooldown-ticks", 20) * 50L;
        plankCooldownMs = plugin.getConfig().getLong("apple-tree.plank-cooldown-ticks", 100) * 50L;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // PlayerInteractEvent fires once per hand — only react to the main hand
        // so a single right-click does not award the reward twice.
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;

        Player player = e.getPlayer();
        String world = plugin.getConfig().getString("apple-tree.world", "world");
        if (!player.getWorld().getName().equals(world)) return;

        if (leafMaterials.contains(block.getType())) {
            handleLeafClick(player, block);
        } else if (logMaterials.contains(block.getType())) {
            handleLogClick(player, block);
        }
    }

    private void handleLeafClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = appleCooldowns.get(uuid);
        if (last != null && now - last < appleCooldownMs) return;
        appleCooldowns.put(uuid, now);

        int baseApples = plugin.getConfig().getInt("apple-tree.base-apples", 1);
        int bonus = plugin.getBoosterManager().getBonus(uuid);
        int total = baseApples + bonus;

        // Award apples via LemonCore
        org.bukkit.plugin.Plugin lcPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore lc) {
            lc.getPlayerDataManager().addApples(uuid, total);
        }

        // Consume booster use
        if (bonus > 0) {
            plugin.getBoosterManager().consumeUse(uuid);
        }

        // Feedback
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.4f);
        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 5, 0.3, 0.3, 0.3, 0);

        String msg = bonus > 0
                ? "<!italic><gradient:#fffb00:#00ff00>+<white>" + total + " <green>✿ <gray>(<yellow>+" + bonus + " Boost<gray>)"
                : "<!italic><gradient:#fffb00:#00ff00>+<white>" + total + " <green>✿";
        player.sendActionBar(MM.deserialize(msg));
    }

    private void handleLogClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = plankCooldowns.get(uuid);
        if (last != null && now - last < plankCooldownMs) return;
        plankCooldowns.put(uuid, now);

        PlankTier tier = plugin.getTreeUpgradeManager().getCurrentTier(uuid);
        int planks = tier.planksPerClick;

        // Award planks via LemonCore
        org.bukkit.plugin.Plugin lcPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lcPlugin instanceof com.lemonpvp.lemoncore.LemonCore lc) {
            lc.getPlayerDataManager().addPlanks(uuid, planks);
        }

        // Feedback
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 0.8f, 1.0f);
        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 8, 0.3, 0.3, 0.3, block.getBlockData());

        player.sendActionBar(MM.deserialize(
                "<!italic><gradient:#8B4513:#D2691E>+<white>" + planks + " <#D2691E>▬ <gray>(" + tier.displayName + ")"));
    }

    /** Whether the given block is part of the apple tree (leaf or log). */
    public boolean isTreeBlock(Block block) {
        if (block == null) return false;
        return leafMaterials.contains(block.getType()) || logMaterials.contains(block.getType());
    }

    public void cleanupPlayer(UUID uuid) {
        appleCooldowns.remove(uuid);
        plankCooldowns.remove(uuid);
    }
}
