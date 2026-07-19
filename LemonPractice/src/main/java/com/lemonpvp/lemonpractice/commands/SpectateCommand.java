package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import com.lemonpvp.lemonpractice.duel.DuelState;
import com.lemonpvp.lemonpractice.gui.SpectateBrowserGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /spectate} — watch running duels:
 * <ul>
 *   <li>{@code /spectate} — open the live-match browser</li>
 *   <li>{@code /spectate <player>} — jump straight to that player's duel</li>
 *   <li>{@code /spectate leave} — stop watching</li>
 * </ul>
 */
public class SpectateCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public SpectateCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }

        if (args.length == 0) {
            new SpectateBrowserGUI(plugin, player).open();
            return true;
        }
        if (args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("stop")) {
            if (plugin.getDuelSpectateManager().isWatching(player.getUniqueId())) {
                plugin.getDuelSpectateManager().leave(player, true);
            } else {
                player.sendMessage(MM.deserialize("<!italic><gray>You aren't spectating a match."));
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        DuelGame game = target != null ? plugin.getDuelManager().getDuel(target.getUniqueId()) : null;
        if (game == null || game.getState() != DuelState.FIGHTING) {
            player.sendMessage(MM.deserialize("<!italic><red>" + args[0] + " <red>isn't fighting right now."));
            return true;
        }
        plugin.getDuelSpectateManager().spectate(player, game);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if ("leave".startsWith(prefix)) out.add("leave");
        for (DuelGame g : plugin.getDuelManager().getActiveGames()) {
            if (g.getState() != DuelState.FIGHTING) continue;
            for (String name : new String[]{g.getPlayer1Name(), g.getPlayer2Name()}) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }
}
