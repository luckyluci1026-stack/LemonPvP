package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AowCodeCommand implements CommandExecutor {

    private final LemonCore plugin;

    public AowCodeCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.code")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-usage",
                    "usage", "/aowcode <name|random26> <rank|coins|killeffect> <value> <max_uses> <duration>"));
            return true;
        }

        String codeName = args[0];
        String rewardType = args[1].toLowerCase();
        String rewardValue = args[2];
        int maxUses;
        long duration;

        try { maxUses = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessagesManager().get("invalid-amount"));
            return true;
        }

        duration = TextUtil.parseDuration(args[4]);
        UUID creatorUuid = sender instanceof Player p ? p.getUniqueId() : null;

        plugin.getCodeManager().createCode(codeName, rewardType, rewardValue, maxUses, duration, creatorUuid)
                .thenAccept(code -> {
                    if (code != null) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getMessagesManager().get("code.create", "code", code)));
                    }
                });
        return true;
    }
}
