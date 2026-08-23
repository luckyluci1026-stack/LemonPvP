package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /report <Spieler> <Grund> - fuer alle, nicht nur fuers Team.
 *
 * Auf einem Schulserver ist das der Weg, wie eine gemobbte Klasse 8b
 * ohne Umweg ueber Discord jemanden erreicht, der gerade online ist.
 * Das Team bekommt die Meldung live, alle anderen sehen nichts davon -
 * eine oeffentliche Anschuldigung waere selbst schon ein Problem.
 */
public final class ReportCommand implements TabExecutor {

    private final BetterSMP plugin;

    public ReportCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player melder)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!melder.hasPermission("bettersmp.report")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.msgs().send(melder, "report.usage");
            return true;
        }
        Player gemeldet = Bukkit.getPlayerExact(args[0]);
        if (gemeldet == null) {
            plugin.msgs().send(melder, "report.not-online", "spieler", args[0]);
            return true;
        }
        if (gemeldet.equals(melder)) {
            plugin.msgs().send(melder, "report.not-yourself");
            return true;
        }
        long wartezeit = plugin.reports().verbleibendeSekunden(melder.getUniqueId());
        if (wartezeit > 0) {
            plugin.msgs().send(melder, "report.cooldown", "sekunden", String.valueOf(wartezeit));
            return true;
        }

        String grund = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length));
        plugin.reports().vermerken(melder, gemeldet, grund);
        plugin.msgs().send(melder, "report.sent", "spieler", gemeldet.getName());

        for (Player teammitglied : Bukkit.getOnlinePlayers()) {
            if (teammitglied.hasPermission("bettersmp.report.receive")) {
                plugin.msgs().send(teammitglied, "report.received",
                        "melder", melder.getName(), "spieler", gemeldet.getName(), "grund", grund);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> namen = new ArrayList<>();
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            if (!spieler.equals(sender)) {
                namen.add(spieler.getName());
            }
        }
        return namen;
    }
}
