package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
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
 * {@code /duel <player> [gamemode]} — challenge a player directly.
 * {@code /duel accept <player>} / {@code /duel deny <player>} — respond.
 */
public class DuelCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public DuelCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String first = args[0].toLowerCase();

        if (first.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(MM.deserialize("<red>Usage: <yellow>/duel accept <player>"));
                return true;
            }
            plugin.getDuelInviteManager().acceptInvite(player, args[1]);
            return true;
        }

        if (first.equals("deny") || first.equals("decline")) {
            if (args.length < 2) {
                player.sendMessage(MM.deserialize("<red>Usage: <yellow>/duel deny <player>"));
                return true;
            }
            plugin.getDuelInviteManager().denyInvite(player, args[1]);
            return true;
        }

        // /duel <player> [gamemode]
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(MM.deserialize("<red>Player <yellow>" + args[0] + "</yellow> is not online."));
            return true;
        }

        String gamemode = (args.length >= 2)
                ? args[1].toLowerCase()
                : defaultGamemode();
        if (gamemode == null) {
            player.sendMessage(MM.deserialize("<red>No duel modes are configured."));
            return true;
        }

        plugin.getDuelInviteManager().sendInvite(player, target, gamemode);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MM.deserialize("<dark_gray><st>                        </st>"));
        player.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00><bold>Duel Commands</bold></gradient>"));
        player.sendMessage(MM.deserialize("<yellow>/duel <player> [mode] <gray>— Challenge"));
        player.sendMessage(MM.deserialize("<yellow>/duel accept <player> <gray>— Accept"));
        player.sendMessage(MM.deserialize("<yellow>/duel deny <player> <gray>— Decline"));
        player.sendMessage(MM.deserialize("<dark_gray><st>                        </st>"));
    }

    private String defaultGamemode() {
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            if (gm.isEnabled()) return gm.getId();
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            // Sub-commands
            for (String sub : List.of("accept", "deny")) {
                if (sub.startsWith(partial)) out.add(sub);
            }
            // Online player names
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player self && self.getUniqueId().equals(p.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName());
            }
            return out;
        }

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny")) {
                if (sender instanceof Player self) {
                    for (String name : plugin.getDuelInviteManager().getPendingSenderNames(self.getUniqueId())) {
                        if (name.toLowerCase().startsWith(partial)) out.add(name);
                    }
                }
                return out;
            }
            // gamemode suggestions for /duel <player> <gamemode>
            for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
                if (gm.isEnabled() && gm.getId().toLowerCase().startsWith(partial)) out.add(gm.getId());
            }
            return out;
        }

        return out;
    }
}
