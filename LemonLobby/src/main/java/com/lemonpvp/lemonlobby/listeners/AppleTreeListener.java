package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.model.PlankTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
    private static final long COMBO_WINDOW_MS = 2_000;
    private static final int  MAX_COMBO = 5;

    private final LemonLobby plugin;
    private final Map<UUID, Long>    appleCooldowns = new HashMap<>();
    private final Map<UUID, Long>    plankCooldowns = new HashMap<>();
    private final Map<UUID, Integer> comboCount     = new HashMap<>();
    private final Map<UUID, Long>    lastClickTime  = new HashMap<>();

    // Cached from config on reload()
    private Set<Material> leafMaterials;
    private Set<Material> logMaterials;
    private long appleCooldownMs;
    private long plankCooldownMs;
    private String worldName;
    private int baseApples;
    private LemonCore lcCache;

    public AppleTreeListener(LemonLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        leafMaterials = new HashSet<>();
        logMaterials  = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("apple-tree.leaf-materials")) {
            try { leafMaterials.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : plugin.getConfig().getStringList("apple-tree.log-materials")) {
            try { logMaterials.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        appleCooldownMs = plugin.getConfig().getLong("apple-tree.apple-cooldown-ticks", 20) * 50L;
        plankCooldownMs = plugin.getConfig().getLong("apple-tree.plank-cooldown-ticks", 100) * 50L;
        worldName   = plugin.getConfig().getString("apple-tree.world", "world");
        baseApples  = plugin.getConfig().getInt("apple-tree.base-apples", 1);
        var p = Bukkit.getPluginManager().getPlugin("LemonCore");
        lcCache = p instanceof LemonCore lc ? lc : null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!e.getPlayer().getWorld().getName().equals(worldName)) return;

        Material type = block.getType();
        Player player = e.getPlayer();

        if (leafMaterials.contains(type)) {
            handleLeafClick(player, block);
        } else if (logMaterials.contains(type)) {
            handleLogClick(player, block);
        }
    }

    private void handleLeafClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long last = appleCooldowns.get(uuid);
        if (last != null && now - last < appleCooldownMs) {
            long rem = appleCooldownMs - (now - last);
            player.sendActionBar(MM.deserialize(
                    "<!italic><red>⏳ <white>" + String.format("%.1f", rem / 1000.0) + "s"));
            return;
        }
        appleCooldowns.put(uuid, now);

        int combo  = advanceCombo(uuid, now);
        int bonus  = plugin.getBoosterManager().getBonus(uuid);
        // Multiplier scales linearly: 1.0x at combo 1 → 2.0x at combo MAX_COMBO
        double mult  = 1.0 + (double)(combo - 1) / MAX_COMBO;
        int total    = (int) Math.round((baseApples + bonus) * mult);

        if (lcCache != null) lcCache.getPlayerDataManager().addApples(uuid, total);
        if (bonus > 0) plugin.getBoosterManager().consumeUse(uuid);

        // Sound: pitch rises with combo
        float pitch = Math.min(2.0f, 1.0f + (combo - 1) * 0.15f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, pitch);

        // Max-combo burst
        if (combo == MAX_COMBO) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        }

        // Particles
        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);
        int pCount = 4 + combo * 2;
        if (bonus > 0) {
            block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, pCount, 0.3, 0.3, 0.3, 0.05);
        } else {
            block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, pCount, 0.3, 0.3, 0.3, 0);
        }

        // Action-bar message
        String comboTag = combo > 1 ? " <gold>×" + combo : "";
        String boostTag = bonus > 0 ? " <gray>(<yellow>Boost +<white>" + bonus + "<gray>)" : "";
        String gradient = combo >= MAX_COMBO ? "<gradient:#fffb00:#ff6600>" : "<green>";
        player.sendActionBar(MM.deserialize(
                "<!italic>" + gradient + "+<white>" + total + " <green>✿" + comboTag + boostTag));
    }

    private void handleLogClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long last = plankCooldowns.get(uuid);
        if (last != null && now - last < plankCooldownMs) {
            long rem = plankCooldownMs - (now - last);
            player.sendActionBar(MM.deserialize(
                    "<!italic><red>⏳ <white>" + String.format("%.1f", rem / 1000.0) + "s"));
            return;
        }
        plankCooldowns.put(uuid, now);

        PlankTier tier   = plugin.getTreeUpgradeManager().getCurrentTier(uuid);
        int planks       = tier.planksPerClick;

        if (lcCache != null) lcCache.getPlayerDataManager().addPlanks(uuid, planks);

        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 0.8f, 1.0f);
        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 12, 0.3, 0.3, 0.3, block.getBlockData());

        player.sendActionBar(MM.deserialize(
                "<!italic><#D2691E>+<white>" + planks + " <#D2691E>▬ <gray>(" + tier.displayName + ")"));
    }

    private int advanceCombo(UUID uuid, long now) {
        Long lastTime = lastClickTime.get(uuid);
        int current   = comboCount.getOrDefault(uuid, 0);
        int next = (lastTime != null && now - lastTime <= COMBO_WINDOW_MS)
                ? Math.min(current + 1, MAX_COMBO)
                : 1;
        comboCount.put(uuid, next);
        lastClickTime.put(uuid, now);
        return next;
    }

    public boolean isTreeBlock(Block block) {
        if (block == null) return false;
        return leafMaterials.contains(block.getType()) || logMaterials.contains(block.getType());
    }

    public void cleanupPlayer(UUID uuid) {
        appleCooldowns.remove(uuid);
        plankCooldowns.remove(uuid);
        comboCount.remove(uuid);
        lastClickTime.remove(uuid);
    }
}
