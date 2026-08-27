package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.gui.ReportGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GReportCommand implements CommandExecutor {

    private final LemonCore plugin;

    public GReportCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) return true;
        if (!admin.hasPermission("lemoncore.admin.report")) {
            admin.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        new ReportGUI(plugin).open(admin);
        return true;
    }
}
