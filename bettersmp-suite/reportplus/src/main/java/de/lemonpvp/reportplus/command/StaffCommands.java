package de.lemonpvp.reportplus.command;

import de.lemonpvp.reportplus.ReportPlus;
import de.lemonpvp.reportplus.gui.Guis;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /reports und /bugreports - die Warteschlangen-GUIs fuers Team. */
public final class StaffCommands implements CommandExecutor {

    private final ReportPlus plugin;

    public StaffCommands(ReportPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (command.getName().equalsIgnoreCase("reports")) {
            if (!spieler.hasPermission("bettersmp.report.receive")) {
                plugin.msgs().send(spieler, "no-permission");
                return true;
            }
            Guis.oeffneReportListe(plugin, spieler, 0);
        } else {
            if (!spieler.hasPermission("bettersmp.bugreport.receive")) {
                plugin.msgs().send(spieler, "no-permission");
                return true;
            }
            Guis.oeffneBugListe(plugin, spieler, 0);
        }
        return true;
    }
}
