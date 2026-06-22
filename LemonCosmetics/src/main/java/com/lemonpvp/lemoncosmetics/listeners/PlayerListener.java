package com.lemonpvp.lemoncosmetics.listeners;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.gui.CosmeticsMainGUI;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class PlayerListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int HOTBAR_SLOT = 8;

    private final LemonCosmetics plugin;

    public PlayerListener(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID joinedUuid = player.getUniqueId();
        plugin.getCosmeticsManager().loadPlayer(joinedUuid)
                .thenAccept(cosmetics -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(joinedUuid);
                    if (p == null) return;
                    plugin.getArmorTrimManager().applyTrimToPlayer(p);
                    plugin.getHatManager().restoreHat(p);
                    plugin.getTagManager().restoreTag(p);
                    if (isLobby()) {
                        giveCosmeticsItem(p);
                    }
                }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getArrowTrailManager().stopAllTrailsForPlayer(player.getUniqueId());
        plugin.getHatManager().removeHat(player);
        plugin.getCosmeticsManager().unloadPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isLobby()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(HOTBAR_SLOT);
        if (item == null || item.getType() != Material.DIAMOND) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!player.getInventory().getItemInMainHand().equals(item)) return;
        event.setCancelled(true);
        new CosmeticsMainGUI(plugin, player).open();
    }

    @EventHandler
    public void onKillEffectReward(com.lemonpvp.lemoncore.events.KillEffectRewardEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;

        String effectId = event.getEffectId();
        if (KillEffectType.fromId(effectId).isEmpty()) {
            plugin.getLogger().warning("Received unknown kill effect id: " + effectId);
            return;
        }

        UUID effectUuid = event.getPlayerUuid();
        plugin.getCosmeticsManager().unlockKillEffect(effectUuid, effectId)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(effectUuid);
                    if (p == null) return;
                    p.sendMessage(MM.deserialize("<green>Unlocked kill effect: <yellow>"
                            + KillEffectType.fromId(effectId).map(KillEffectType::getDisplayName).orElse(effectId)
                            + "</yellow>!"));
                }));
    }

    private void giveCosmeticsItem(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<aqua>Cosmetics"));
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(HOTBAR_SLOT, item);
    }

    private boolean isLobby() {
        return "LOBBY".equalsIgnoreCase(plugin.getConfig().getString("server-type", "LOBBY"));
    }
}
