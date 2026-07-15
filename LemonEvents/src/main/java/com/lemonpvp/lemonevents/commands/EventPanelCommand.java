package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.gui.OwnerPanelGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /eventpanel} — opens the owner control panel for hosting official
 * events and tournaments (separate from the {@code /host} creator flow).
 */
public class EventPanelCommand implements CommandExecutor {

    private final LemonEvents plugin;

    public EventPanelCommand(LemonEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("lemonevents.owner")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission."));
            return true;
        }
        new OwnerPanelGUI(plugin, player).open();
        return true;
    }
}
