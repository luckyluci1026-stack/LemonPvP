package de.lemonpvp.reportplus.command;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.gui.Guis;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /report [Spieler] - ohne Ziel oeffnet sich ein Spieler-Picker, mit
 * Namen geht's direkt zur Grund-Auswahl. Ersetzt BetterSMPs Text-/report
 * (siehe loadbefore in plugin.yml).
 */
public final class ReportCommand implements TabExecutor {

    private final ReportPlus plugin;

    public ReportCommand(ReportPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!spieler.hasPermission("bettersmp.report")) {
            plugin.msgs().send(spieler, "no-permission");
            return true;
        }
        if (args.length == 0) {
            Guis.oeffnePlayerPicker(plugin, spieler, plugin.msgs().raw("report.picker-title"));
            return true;
        }
        Player ziel = Bukkit.getPlayerExact(args[0]);
        if (ziel == null) {
            plugin.msgs().send(spieler, "report.not-online", "spieler", args[0]);
            return true;
        }
        plugin.reportFlow().starteMitZiel(spieler, ziel);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
