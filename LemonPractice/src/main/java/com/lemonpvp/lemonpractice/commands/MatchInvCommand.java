package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.MatchInventoryGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /matchinv} — opens the post-match loadout viewer, showing the last
 * duel's inventories (yours and your opponent's). Available for ~1 minute after
 * a match; the post-match message offers a clickable link to it.
 */
public class MatchInvCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public MatchInvCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (plugin.getPostMatchManager().get(player.getUniqueId()) == null) {
            player.sendMessage(MM.deserialize("<!italic><gray>No recent match loadouts to review."));
            return true;
        }
        new MatchInventoryGUI(plugin, player).open();
        return true;
    }
}
