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
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        // /p <message> shortcut for party chat
        if (label.equalsIgnoreCase("p") || label.equalsIgnoreCase("partychat")) {
            if (args.length == 0) {
                player.sendMessage(MM.deserialize("<red>Verwendung: /p <Nachricht>"));
                return true;
            }
            plugin.getPartyManager().chat(player, String.join(" ", args));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party invite <Spieler>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(MM.deserialize("<red>Spieler <yellow>" + args[1] + "</yellow> ist nicht online."));
                    return true;
                }
                plugin.getPartyManager().invite(player, target);
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party accept <Spieler>"));
                    return true;
                }
                plugin.getPartyManager().accept(player, args[1]);
            }
            case "deny", "decline" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party deny <Spieler>"));
                    return true;
                }
                plugin.getPartyManager().deny(player, args[1]);
            }
            case "leave" -> plugin.getPartyManager().leave(player);
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party kick <Spieler>"));
                    return true;
                }
                plugin.getPartyManager().kick(player, args[1]);
            }
            case "disband" -> plugin.getPartyManager().disband(player);
            case "list", "info" -> plugin.getPartyManager().list(player);
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party promote <Spieler>"));
                    return true;
                }
                plugin.getPartyManager().promote(player, args[1]);
            }
            case "queue" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party queue <Modus>"));
                    return true;
                }
                String gm = args[1].toLowerCase();
                if (plugin.getGamemodeManager().getGamemode(gm) == null) {
                    player.sendMessage(MM.deserialize("<red>Unbekannter Modus: <yellow>" + gm));
                    return true;
                }
                plugin.getPartyManager().queueParty(player, gm);
            }
            case "chat", "c" -> {
                if (args.length < 2) {
                    player.sendMessage(MM.deserialize("<red>Verwendung: <yellow>/party chat <Nachricht>"));
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
        player.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00><bold>Party-Befehle</bold></gradient>"));
        player.sendMessage(Component.empty());
        player.sendMessage(MM.deserialize("<yellow>/party invite <Spieler> <gray>— Einladen"));
        player.sendMessage(MM.deserialize("<yellow>/party accept <Spieler> <gray>— Einladung annehmen"));
        player.sendMessage(MM.deserialize("<yellow>/party deny <Spieler>   <gray>— Einladung ablehnen"));
        player.sendMessage(MM.deserialize("<yellow>/party leave             <gray>— Party verlassen"));
        player.sendMessage(MM.deserialize("<yellow>/party kick <Spieler>   <gray>— Spieler kicken"));
        player.sendMessage(MM.deserialize("<yellow>/party disband          <gray>— Party auflösen"));
        player.sendMessage(MM.deserialize("<yellow>/party promote <Spieler><gray>— Leader übertragen"));
        player.sendMessage(MM.deserialize("<yellow>/party list             <gray>— Mitglieder anzeigen"));
        player.sendMessage(MM.deserialize("<yellow>/party queue <Modus>   <gray>— Alle queuen"));
        player.sendMessage(MM.deserialize("<yellow>/p <Nachricht>          <gray>— Party-Chat"));
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
