package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.gui.SettingsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /settings - oeffnet das BetterSMP-Einstellungs-GUI.
 */
public final class SettingsCommand implements CommandExecutor {

    private final BetterSMP plugin;

    public SettingsCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("bettersmp.settings")) {
            plugin.msgs().send(player, "no-permission");
            return true;
        }
        player.openInventory(new SettingsGUI(plugin).getInventory());
        return true;
    }
}
