package de.lemonpvp.antiswear.command;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.Treffer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** /antiswear reload | check <Text> | punkte <Spieler> | reset <Spieler> */
public final class AntiSwearCommand implements TabExecutor {

    private final AntiSwear plugin;

    public AntiSwearCommand(AntiSwear plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msgs().format("hilfe"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.ladeAlles();
                plugin.msgs().send(sender, "reloaded");
            }
            case "check" -> pruefeText(sender, args);
            case "punkte" -> zeigePunkte(sender, args);
            case "reset" -> setzeZurueck(sender, args);
            default -> sender.sendMessage(plugin.msgs().format("hilfe"));
        }
        return true;
    }

    private void pruefeText(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msgs().format("check-usage"));
            return;
        }
        String text = String.join(" ", Arrays.asList(args).subList(1, args.length));
        List<Treffer> treffer = plugin.filter().pruefen(text);
        if (treffer.isEmpty()) {
            plugin.msgs().send(sender, "check-sauber");
            return;
        }
        int punkte = treffer.stream().mapToInt(Treffer::punkte).sum();
        String woerter = treffer.stream().map(Treffer::wort).distinct().collect(Collectors.joining(", "));
        plugin.msgs().send(sender, "check-treffer",
                "woerter", woerter, "punkte", String.valueOf(punkte), "zensiert", plugin.filter().zensieren(text));
    }

    private void zeigePunkte(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msgs().format("punkte-usage"));
            return;
        }
        OfflinePlayer ziel = Bukkit.getOfflinePlayer(args[1]);
        int punkte = plugin.strikes().aktuellePunkte(ziel.getUniqueId());
        plugin.msgs().send(sender, "punkte-anzeige", "spieler", args[1], "punkte", String.valueOf(punkte));
    }

    private void setzeZurueck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msgs().format("reset-usage"));
            return;
        }
        OfflinePlayer ziel = Bukkit.getOfflinePlayer(args[1]);
        plugin.strikes().zuruecksetzen(ziel.getUniqueId());
        plugin.msgs().send(sender, "reset-ok", "spieler", args[1]);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "check", "punkte", "reset");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("punkte") || args[0].equalsIgnoreCase("reset"))) {
            List<String> namen = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                namen.add(online.getName());
            }
            return namen;
        }
        return List.of();
    }
}
