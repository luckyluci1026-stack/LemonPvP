package de.lemonpvp.betterrtp.command;

import de.lemonpvp.betterrtp.BetterRTP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * /betterrtp reload|version
 */
public final class AdminCommand implements CommandExecutor {

    private final BetterRTP plugin;

    public AdminCommand(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("betterrtp.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "admin-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.msgs().send(sender, "reloaded");
            }
            case "version" -> sender.sendMessage(plugin.msgs().format("prefix")
                    .append(net.kyori.adventure.text.Component.text(
                            "BetterRTP " + plugin.getPluginMeta().getVersion())));
            default -> plugin.msgs().send(sender, "admin-usage");
        }
        return true;
    }
}
