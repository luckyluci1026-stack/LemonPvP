package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles {@code /party} with all sub-commands and {@code /p} (party chat shortcut).
 */
public class PartyCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final List<String> SUBS = List.of(
            "invite", "accept", "deny", "leave", "kick", "disband", "list", "info",
            "promote", "queue", "chat");

    private final LemonPractice plugin;

    public PartyCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        // /p <message> shortcut for party chat
        if (label.equalsIgnoreCase("p") || label.equalsIgnoreCase("partychat")) {
            if (args.length == 0) {
                player.sendMessage(MM.deserialize("<red>Usage: /p <message>"));
                return true;
            }
            plugin.getPartyManager().chat(player, String.join(" ", args));
            return true;
        }

        // Bare /party (or /party gui) opens the click-driven panel; the
        // subcommands keep working as a fallback.
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            new com.lemonpvp.lemonpractice.gui.PartyGUI(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party invite <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(MM.deserialize("<red>Player <yellow>" + args[1] + "</yellow> is not online."));
                    return true;
                }
                plugin.getPartyManager().invite(player, target);
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party accept <player>"));
                    return true;
                }
                plugin.getPartyManager().accept(player, args[1]);
            }
            case "deny", "decline" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party deny <player>"));
                    return true;
                }
                plugin.getPartyManager().deny(player, args[1]);
            }
            case "leave" -> plugin.getPartyManager().leave(player);
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party kick <player>"));
                    return true;
                }
                plugin.getPartyManager().kick(player, args[1]);
            }
            case "disband" -> plugin.getPartyManager().disband(player);
            case "list", "info" -> plugin.getPartyManager().list(player);
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party promote <player>"));
                    return true;
                }
                plugin.getPartyManager().promote(player, args[1]);
            }
            case "queue" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party queue <mode>"));
                    return true;
                }
                String gm = args[1].toLowerCase();
                if (plugin.getGamemodeManager().getGamemode(gm) == null) {
                    player.sendMessage(MM.deserialize("<red>Unknown mode: <yellow>" + gm));
                    return true;
                }
                plugin.getPartyManager().queueParty(player, gm);
            }
            case "chat", "c" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Usage: <yellow>/party chat <message>"));
                    return true;
                }
                plugin.getPartyManager().chat(player,
                        String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            }
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MM.deserialize("<dark_gray><st>                                        </st>"));
        player.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00><bold>Party Commands</bold></gradient>"));
        player.sendMessage(Component.empty());
        player.sendMessage(MM.deserialize("<yellow>/party invite <player> <gray>— Invite"));
        player.sendMessage(MM.deserialize("<yellow>/party accept <player> <gray>— Accept invite"));
        player.sendMessage(MM.deserialize("<yellow>/party deny <player>   <gray>— Decline invite"));
        player.sendMessage(MM.deserialize("<yellow>/party leave             <gray>— Leave party"));
        player.sendMessage(MM.deserialize("<yellow>/party kick <player>   <gray>— Kick player"));
        player.sendMessage(MM.deserialize("<yellow>/party disband          <gray>— Disband party"));
        player.sendMessage(MM.deserialize("<yellow>/party promote <player><gray>— Transfer leader"));
        player.sendMessage(MM.deserialize("<yellow>/party list             <gray>— Show members"));
        player.sendMessage(MM.deserialize("<yellow>/party queue <mode>   <gray>— Queue everyone"));
        player.sendMessage(MM.deserialize("<yellow>/p <message>          <gray>— Party chat"));
        player.sendMessage(MM.deserialize("<dark_gray><st>                                        </st>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (alias.equalsIgnoreCase("p") || alias.equalsIgnoreCase("partychat")) return out;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBS) {
                if (sub.startsWith(partial)) out.add(sub);
            }
            return out;
        }

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "invite" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (sender instanceof Player self
                                && self.getUniqueId().equals(p.getUniqueId())) continue;
                        if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName());
                    }
                }
                case "accept" -> {
                    if (sender instanceof Player self) {
                        for (String name : plugin.getPartyManager()
                                .getPendingInviterNames(self.getUniqueId())) {
                            if (name.toLowerCase().startsWith(partial)) out.add(name);
                        }
                    }
                }
                case "kick", "promote" -> {
                    if (sender instanceof Player self) {
                        for (String name : plugin.getPartyManager()
                                .getMemberNames(self.getUniqueId())) {
                            if (name.toLowerCase().startsWith(partial)) out.add(name);
                        }
                    }
                }
                case "deny", "decline" -> {
                    if (sender instanceof Player self) {
                        for (String name : plugin.getPartyManager()
                                .getPendingInviterNames(self.getUniqueId())) {
                            if (name.toLowerCase().startsWith(partial)) out.add(name);
                        }
                    }
                }
                case "queue" -> {
                    for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
                        if (gm.isEnabled() && gm.getId().toLowerCase().startsWith(partial))
                            out.add(gm.getId());
                    }
                }
            }
        }

        return out;
    }
}
