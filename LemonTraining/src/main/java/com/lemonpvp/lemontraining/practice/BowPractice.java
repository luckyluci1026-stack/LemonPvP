package com.lemonpvp.lemontraining.practice;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BowPractice extends AbstractPractice {

    private static final Random RANDOM = new Random();
    private final List<Location> activeTargetLocations = new ArrayList<>();

    public BowPractice(LemonTraining plugin, Player player, PracticeSession session) {
        super(plugin, player, session);
    }

    @Override
    public void start() {
        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.BOW);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.getInventory().clear();

        // Bow – Unbreaking 10, unbreakable
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            bowMeta.addEnchant(Enchantment.UNBREAKING, 10, true);
            bowMeta.setUnbreakable(true);
            bow.setItemMeta(bowMeta);
        }
        player.getInventory().setItem(0, bow);

        // Crossbow – Quick Charge III, Unbreaking 10, unbreakable
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta cbMeta = crossbow.getItemMeta();
        if (cbMeta != null) {
            cbMeta.addEnchant(Enchantment.UNBREAKING, 10, true);
            cbMeta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
            cbMeta.setUnbreakable(true);
            crossbow.setItemMeta(cbMeta);
        }
        player.getInventory().setItem(1, crossbow);

        // 64 arrows in slots 2–8
        for (int i = 2; i <= 7; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.ARROW, 64));
        }

        applyPracticeEffects();
        giveLeaveItem();

        // Spawn initial target blocks
        spawnAllTargets();

        // Arrow refill task every 20 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            for (int i = 2; i <= 7; i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot == null || slot.getType() != Material.ARROW) {
                    player.getInventory().setItem(i, new ItemStack(Material.ARROW, 64));
                } else if (slot.getAmount() < 64) {
                    slot.setAmount(64);
                }
            }
        }, 20L, 20L));

        // Boundary check every 5 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getArenaManager().isInsideArena(player.getLocation(), PracticeMode.BOW)) {
                Location spawnBack = plugin.getArenaManager().getSpawnLocation(PracticeMode.BOW);
                if (spawnBack != null) player.teleport(spawnBack);
            }
        }, 5L, 5L));
    }

    private void spawnAllTargets() {
        List<BlockVector3> positions = plugin.getArenaManager().getBowTargetPositions();
        activeTargetLocations.clear();
        for (BlockVector3 bv : positions) {
            Location loc = toLocation(bv);
            if (loc != null) {
                loc.getBlock().setType(Material.TARGET);
                activeTargetLocations.add(loc);
            }
        }
    }

    private Location toLocation(BlockVector3 bv) {
        String worldName = plugin.getConfig().getString("arenas.bow.world", "training_world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, bv.x(), bv.y(), bv.z());
    }

    public boolean isBowTargetLocation(Location loc) {
        for (Location target : activeTargetLocations) {
            if (target.getBlockX() == loc.getBlockX()
                    && target.getBlockY() == loc.getBlockY()
                    && target.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public void onTargetHit(BlockVector3 bv) {
        Location loc = toLocation(bv);
        if (loc != null) onTargetHit(loc);
    }

    public void refillArrows() {
        if (!player.isOnline()) return;
        for (int i = 2; i <= 7; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType() != Material.ARROW || slot.getAmount() < 64) {
                player.getInventory().setItem(i, new ItemStack(Material.ARROW, 64));
            }
        }
    }

    public void onTargetHit(Location targetLoc) {
        session.incrementHits();
        // Remove the target block
        targetLoc.getBlock().setType(Material.AIR);
        activeTargetLocations.removeIf(l ->
                l.getBlockX() == targetLoc.getBlockX()
                        && l.getBlockY() == targetLoc.getBlockY()
                        && l.getBlockZ() == targetLoc.getBlockZ());

        // Update action bar
        player.sendActionBar(MM.deserialize("<green>Hits: <white>" + session.getHits() + "</white></green>"));

        // Respawn at a different random target position after 40 ticks
        List<BlockVector3> allPositions = plugin.getArenaManager().getBowTargetPositions();
        List<BlockVector3> available = new ArrayList<>(allPositions);
        // Remove already-active positions
        for (Location active : activeTargetLocations) {
            available.removeIf(bv ->
                    bv.x() == active.getBlockX()
                            && bv.y() == active.getBlockY()
                            && bv.z() == active.getBlockZ());
        }

        if (available.isEmpty()) {
            available = new ArrayList<>(allPositions);
        }

        BlockVector3 respawnPos = available.get(RANDOM.nextInt(available.size()));
        Location respawnLoc = toLocation(respawnPos);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (respawnLoc != null) {
                respawnLoc.getBlock().setType(Material.TARGET);
                activeTargetLocations.add(respawnLoc);
            }
        }, 40L);
    }

    /** Cleans up target blocks and inventory without sending the player to lobby. */
    public void cleanup() {
        for (Location loc : activeTargetLocations) {
            if (loc.getBlock().getType() == Material.TARGET) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        activeTargetLocations.clear();
        player.getInventory().clear();
    }

    @Override
    public void end() {
        cleanup();
        cancelTasks();
        sendToLobby();
    }
}
