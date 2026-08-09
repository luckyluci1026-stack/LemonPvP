package de.lemonpvp.bettersmp.gui;

import de.lemonpvp.bettersmp.BetterSMP;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Klick-Logik für das /settings-GUI.
 */
public final class SettingsListener implements Listener {

    private final BetterSMP plugin;

    public SettingsListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsGUI gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(gui.getInventory())) {
            return;
        }
        int slot = event.getSlot();

        SettingsGUI.Toggle toggle = gui.toggleAt(slot);
        if (toggle != null) {
            boolean now = !plugin.getConfig().getBoolean(toggle.path(), true);
            plugin.getConfig().set(toggle.path(), now);
            plugin.saveConfig();
            plugin.reloadModules();
            gui.render();
            click(player, now);
            return;
        }

        SettingsGUI.Action action = gui.actionAt(slot);
        if (action == null) {
            return;
        }
        switch (action) {
            case RELOAD -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.reloadModules();
                gui.render();
                plugin.msgs().send(player, "reloaded");
                click(player, true);
            }
            case INSTALL -> {
                player.closeInventory();
                plugin.installer().installAsync(player);
            }
            case RANKS -> {
                player.closeInventory();
                plugin.setupRanks(player);
            }
            case CLOSE -> player.closeInventory();
        }
    }

    private void click(Player player, boolean positive) {
        player.playSound(Sound.sound(Key.key("minecraft:ui.button.click"),
                Sound.Source.MASTER, 0.6f, positive ? 1.4f : 0.9f));
    }
}
