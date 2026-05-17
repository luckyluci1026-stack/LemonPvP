package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RecordingCommand implements CommandExecutor {

    private final LemonCore plugin;

    public RecordingCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.recording")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data == null) return true;

        boolean newState = !data.isRecordingMode();
        data.setRecordingMode(newState);

        if (newState) {
            player.sendMessage(plugin.getMessagesManager().get("recording.enabled"));
            // Update nameplate - set a red camera prefix in display name
            player.customName(com.lemonpvp.lemoncore.util.TextUtil.parse(
                    "<red>📹</red> " + data.getDisplayName()));
        } else {
            player.sendMessage(plugin.getMessagesManager().get("recording.disabled"));
            player.customName(com.lemonpvp.lemoncore.util.TextUtil.parse(data.getDisplayName()));
        }

        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        return true;
    }
}
