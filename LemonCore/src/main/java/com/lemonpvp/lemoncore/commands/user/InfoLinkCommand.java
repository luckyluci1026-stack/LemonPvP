package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Sends a configurable, clickable info link. One instance per link command
 * ({@code /discord}, {@code /store}); each is bound to a config key + accent.
 */
public class InfoLinkCommand implements CommandExecutor {

    private final LemonCore plugin;
    private final String configKey;
    private final String defaultUrl;
    private final String label;
    private final String accent;

    public InfoLinkCommand(LemonCore plugin, String configKey, String defaultUrl, String label, String accent) {
        this.plugin = plugin;
        this.configKey = configKey;
        this.defaultUrl = defaultUrl;
        this.label = label;
        this.accent = accent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String lbl, String[] args) {
        String url = plugin.getConfig().getString(configKey, defaultUrl);
        sender.sendMessage(TextUtil.parse(
                "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray> "
                        + "<gray>" + label + ": "
                        + "<click:open_url:'" + url + "'>"
                        + "<hover:show_text:'<green>Click to open " + label + "'>"
                        + "<" + accent + "><underlined>" + url + "</underlined></hover></click>"));
        return true;
    }
}
