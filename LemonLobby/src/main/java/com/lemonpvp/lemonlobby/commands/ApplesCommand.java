package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.util.FormatUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ApplesCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonLobby plugin;

    public ApplesCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        LemonCore lc = getLemonCore();
        if (lc == null) {
            player.sendMessage(MM.deserialize("<!italic><red>Economy-System nicht verfügbar."));
            return true;
        }
        PlayerData data = lc.getPlayerDataManager().getCached(player.getUniqueId());
        String formatted = data != null ? FormatUtil.formatAmount(data.getApples()) : "…";
        player.sendMessage(MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray>"
                + " <gray>Deine Äpfel: <green>✿ <white>" + formatted));
        return true;
    }

    private LemonCore getLemonCore() {
        var p = Bukkit.getPluginManager().getPlugin("LemonCore");
        return p instanceof LemonCore lc ? lc : null;
    }
}
