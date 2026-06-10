package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        plugin.getDiscordLinkManager().getLinkedAccount(uuid).thenAccept(link -> {
            if (link != null) {
                String discordName = link.discordUsername;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage(MM.deserialize(
                            "<gray>Already linked to <white>" + discordName
                            + "</white>. Use /unlink to remove.</gray>"));
                });
                return;
            }
            plugin.getDiscordLinkManager()
                    .generateCode(uuid, playerName)
                    .thenAccept(code -> {
                        String invite = plugin.getConfig().getString(
                                "discord.invite", "https://discord.gg/lemonpvp");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p == null) return;
                            if (code == null) {
                                p.sendMessage(MM.deserialize(
                                        "<red>Failed to generate code. Please try again.</red>"));
                                return;
                            }
                            p.sendMessage(MM.deserialize(" "));
                            p.sendMessage(MM.deserialize(
                                    "<bold><gradient:#fffb00:#00ff00>>></gradient>"
                                    + " Discord Link "
                                    + "<gradient:#fffb00:#00ff00><<</gradient></bold>"));
                            p.sendMessage(MM.deserialize(
                                    "<white>Go to our Discord and type:</white>"));
                            p.sendMessage(MM.deserialize(
                                    "<bold><yellow>/verify " + code + "</yellow></bold>"));
                            p.sendMessage(MM.deserialize(
                                    "<gray>Code expires in 10 minutes.</gray>"));
                            p.sendMessage(MM.deserialize("<aqua>" + invite + "</aqua>"));
                            p.sendMessage(MM.deserialize(" "));
                        });
                    });
        });
        return true;
    }
}
