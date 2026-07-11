package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.gui.SettingsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingsCommand implements CommandExecutor {

    private final LemonCore plugin;

    public SettingsCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Not cached yet (e.g. right after join) — load, then open on the main thread.
        if (plugin.getPlayerDataManager().getCached(player.getUniqueId()) == null) {
            java.util.UUID uuid = player.getUniqueId();
            plugin.getPlayerDataManager().loadPlayer(uuid, player.getName())
                    .thenAccept(d -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = org.bukkit.Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) new SettingsGUI(plugin, p).open();
                    }));
            return true;
        }

        new SettingsGUI(plugin, player).open();
        return true;
    }
}
