package de.lemonpvp.reportplus.command;

import de.lemonpvp.reportplus.ReportPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /bugreport - Kategorie per GUI, Beschreibung per Chat (siehe BugFlow). */
public final class BugReportCommand implements CommandExecutor {

    private final ReportPlus plugin;

    public BugReportCommand(ReportPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!spieler.hasPermission("bettersmp.bugreport")) {
            plugin.msgs().send(spieler, "no-permission");
            return true;
        }
        plugin.bugFlow().starte(spieler);
        return true;
    }
}
