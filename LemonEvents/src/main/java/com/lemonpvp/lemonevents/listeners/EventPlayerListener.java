package com.lemonpvp.lemonevents.listeners;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.game.horror.EscapeGame;
import com.lemonpvp.lemonevents.game.horror.HuntGame;
import com.lemonpvp.lemonevents.game.horror.MafiaGame;
import com.lemonpvp.lemonevents.game.lemonroyale.LemonRoyaleGame;
import com.lemonpvp.lemonevents.game.pvp.FreeForAllGame;
import com.lemonpvp.lemonevents.game.pvp.TeamFightGame;
import com.lemonpvp.lemonevents.game.pvp.TournamentGame;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EventPlayerListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public EventPlayerListener(LemonEvents plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AbstractGame game = plugin.getEventManager().getGameForPlayer(player.getUniqueId());
        if (game == null) return;

        event.setDeathMessage(null); // suppress vanilla death message
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = player.getKiller();

        if (game instanceof TournamentGame tg) {
            tg.onFighterDeath(player);
        } else if (game instanceof TeamFightGame tf) {
            tf.onPlayerDeath(player);
        } else if (game instanceof FreeForAllGame ffa) {
            if (killer != null) ffa.onPlayerKill(killer, player);
        } else {
            // Generic: eliminate player
            game.eliminate(player.getUniqueId(), killer != null ? killer.getUniqueId() : null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AbstractGame game = plugin.getEventManager().getGameForPlayer(event.getPlayer().getUniqueId());
        if (game != null) game.handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        AbstractGame game = plugin.getEventManager().getGameForPlayer(player.getUniqueId());
        if (game == null) return;

        // LemonRoyale custom items
        if (game instanceof LemonRoyaleGame lrg) {
            ItemStack item = event.getItem();
            if (item == null) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) return;
            int cmd = meta.getCustomModelData();

            if (cmd == 7002 && item.getType() == Material.POTION) {
                // Citrus Shield: activate on drink (any interact)
                event.setCancelled(true);
                LemonRoyaleGame.applyCitrusShield(player);
                item.setAmount(item.getAmount() - 1);
            } else if (cmd == 7003 && item.getType() == Material.PAPER
                    && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                // Grove Salve: 2 second use time simulated by delay
                event.setCancelled(true);
                player.sendActionBar(MM.deserialize("<green>Applying Grove Salve..."));
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        LemonRoyaleGame.applyGroveSalve(player);
                        item.setAmount(item.getAmount() - 1);
                    }
                }, 40L); // 2 seconds
            } else if (cmd == 7004 && item.getType() == Material.TORCH) {
                // Lemon Drop: mark target location
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    event.setCancelled(true);
                    LemonRoyaleGame.activateLemonDrop(player, event.getClickedBlock().getLocation().add(0, 1, 0), plugin);
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }

        // Horror Escape: detect exit block
        if (game instanceof EscapeGame eg
                && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL)) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.EMERALD_BLOCK) {
                eg.onPlayerExit(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        AbstractGame game = plugin.getEventManager().getGameForPlayer(attacker.getUniqueId());
        if (game == null) return;

        // Venom Bomb hit detection (CMD 7001 snowball handled by ProjectileHit event, see below)

        // Hunt: track hunter hits
        if (game instanceof HuntGame hg) {
            hg.onHunterHit(attacker, victim);
        }
    }

    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player shooter)) return;

        AbstractGame game = plugin.getEventManager().getGameForPlayer(shooter.getUniqueId());
        if (!(game instanceof LemonRoyaleGame)) return;

        // Check if it's a Venom Bomb (CMD 7001)
        // Snowballs don't carry ItemStack metadata to the projectile entity directly in 1.21.4,
        // so we track using the shooter's in-hand item before throw
        // For simplicity, treat ALL snowballs thrown in a LemonRoyale game as Venom Bombs
        if (event.getHitEntity() instanceof Player || event.getHitBlock() != null) {
            LemonRoyaleGame.applyVenomBomb(snowball.getLocation(), shooter);
        }
    }

    @EventHandler
    public void onChestOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        AbstractGame game = plugin.getEventManager().getGameForPlayer(player.getUniqueId());
        if (!(game instanceof LemonRoyaleGame lrg)) return;

        org.bukkit.block.Block block = player.getWorld().getBlockAt(
                player.getTargetBlockExact(5) != null
                ? player.getTargetBlockExact(5).getLocation()
                : player.getLocation());
        if (block.getType() == Material.CHEST) {
            lrg.onChestOpen(player, block.getLocation());
        }
    }
}
