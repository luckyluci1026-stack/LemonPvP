package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public LinkCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.use.link")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission.</red>"));
            return true;
        }

        plugin.getDiscordLinkManager().getLinkedAccount(player.getUniqueId()).thenAccept(link -> {
            if (link != null) {
                player.sendMessage(MM.deserialize(
                        "<gray>Already linked to <white>" + link.discordUsername
                        + "</white>. Use /unlink to remove.</gray>"));
                return;
            }
            plugin.getDiscordLinkManager()
                    .generateCode(player.getUniqueId(), player.getName())
                    .thenAccept(code -> {
                        if (code == null) {
                            player.sendMessage(MM.deserialize(
                                    "<red>Failed to generate code. Please try again.</red>"));
                            return;
                        }
                        String invite = plugin.getConfig().getString(
                                "discord.invite", "https://discord.gg/lemonpvp");
                        player.sendMessage(MM.deserialize(" "));
                        player.sendMessage(MM.deserialize(
                                "<bold><gradient:#fffb00:#00ff00>>></gradient>"
                                + " Discord Link "
                                + "<gradient:#fffb00:#00ff00><<</gradient></bold>"));
                        player.sendMessage(MM.deserialize(
                                "<white>Go to our Discord and type:</white>"));
                        player.sendMessage(MM.deserialize(
                                "<bold><yellow>/verify " + code + "</yellow></bold>"));
                        player.sendMessage(MM.deserialize(
                                "<gray>Code expires in 10 minutes.</gray>"));
                        player.sendMessage(MM.deserialize("<aqua>" + invite + "</aqua>"));
                        player.sendMessage(MM.deserialize(" "));
                    });
        });
        return true;
    }
}
