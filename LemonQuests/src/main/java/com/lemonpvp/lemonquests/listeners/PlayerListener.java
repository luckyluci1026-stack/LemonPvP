package com.lemonpvp.lemonquests.listeners;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.gui.QuestGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonQuests plugin;
    private final NamespacedKey questItemKey;

    public PlayerListener(LemonQuests plugin) {
        this.plugin = plugin;
        this.questItemKey = new NamespacedKey(plugin, "quest_item");
    }

    // -------------------------------------------------------------------------
    // Join / quit
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data and quest progress asynchronously
        plugin.getPlayerDataManager().loadPlayer(player.getUniqueId()).thenRun(() ->
                plugin.getQuestManager().loadProgressForPlayer(player.getUniqueId()));

        // Give the lobby hotbar item on the main thread after data is loaded
        // We schedule it a tick later so the player is fully initialized
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) setupHotbar(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().saveAndUnload(player.getUniqueId());
        plugin.getQuestManager().unloadPlayer(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Hotbar interaction — open GUI when quest item is right-clicked
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;
        if (!isQuestItem(item)) return;

        // Open on right-click (both air and block)
        org.bukkit.event.block.Action action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);
        new QuestGUI(plugin, player).open();
    }

    // -------------------------------------------------------------------------
    // Hotbar setup
    // -------------------------------------------------------------------------

    /**
     * Places the quests SMITHING_TABLE item in the player's hotbar slot 0.
     */
    public void setupHotbar(Player player) {
        ItemStack questItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = questItem.getItemMeta();

        meta.displayName(MM.deserialize("<gradient:#fffb00:#00ff00>Quests</gradient>"));

        // PDC tag so we can identify it on right-click
        meta.getPersistentDataContainer().set(questItemKey, PersistentDataType.STRING, "true");

        questItem.setItemMeta(meta);
        player.getInventory().setItem(0, questItem);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the given item has the "lemonquests:quest_item" PDC tag.
     */
    private boolean isQuestItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return "true".equals(pdc.get(questItemKey, PersistentDataType.STRING));
    }
}
