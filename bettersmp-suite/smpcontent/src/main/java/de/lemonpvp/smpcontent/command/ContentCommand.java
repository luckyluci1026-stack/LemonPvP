package de.lemonpvp.smpcontent.command;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /smpcontent give|list|reload
 */
public final class ContentCommand implements TabExecutor {

    private final SMPContent plugin;

    public ContentCommand(SMPContent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smpcontent.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.registry().load();

                ConfigProblem.Report problem = plugin.configProblem() != null
                        ? plugin.configProblem()
                        : plugin.msgs().problem();
                if (problem == null && !plugin.registry().problems().isEmpty()) {
                    problem = plugin.registry().problems().get(0);
                }
                if (problem == null) {
                    plugin.msgs().send(sender, "reloaded");
                } else {
                    plugin.msgs().send(sender, "config-error",
                            "file", problem.file(),
                            "line", String.valueOf(problem.line()),
                            "hint", ConfigProblem.safeForChat(problem.hint()));
                    for (String line : problem.context()) {
                        sender.sendMessage(Text.mm("<dark_gray>"
                                + ConfigProblem.safeForChat(line) + "</dark_gray>"));
                    }
                }
            }
            case "list", "search", "suche" -> {
                String search = args.length > 1
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                        : "";
                if (sender instanceof Player player) {
                    plugin.gui().open(player, search);
                } else {
                    plugin.msgs().send(sender, "list-console",
                            "list", String.join(", ", plugin.registry().entries().keySet()));
                }
            }
            case "give" -> give(sender, args);
            case "pack" -> {
                plugin.msgs().send(sender, "pack-building");
                var result = plugin.pack().build();
                if (!result.ok()) {
                    plugin.msgs().send(sender, "pack-failed", "error", result.message());
                } else {
                    plugin.msgs().send(sender, "pack-done",
                            "items", String.valueOf(result.items()),
                            "blocks", String.valueOf(result.blocks()),
                            "models", String.valueOf(result.customModels()));
                    if (result.missing() > 0) {
                        plugin.msgs().send(sender, "pack-missing",
                                "count", String.valueOf(result.missing()),
                                "details", result.message());
                    }
                }
            }
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msgs().send(sender, "give-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.msgs().send(sender, "player-not-found");
            return;
        }
        CustomEntry entry = plugin.registry().get(args[2]);
        if (entry == null) {
            plugin.msgs().send(sender, "unknown-content", "id", args[2],
                    "list", String.join(", ", plugin.registry().entries().keySet()));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        // Mehr als ein Stapel wird auf mehrere aufgeteilt, Reste fallen zu Boden
        int left = amount;
        while (left > 0) {
            int batch = Math.min(64, left);
            left -= batch;
            plugin.registry().give(target, entry, batch);
        }
        plugin.msgs().send(sender, "given", "amount", String.valueOf(amount),
                "id", entry.id(), "player", target.getName());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "list", "pack", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Bei mehreren hundert Ids nicht alles schicken: erst was mit dem
            // Getippten anfängt, dann was es enthält - und höchstens 60 Stück.
            String typed = args[2].toLowerCase(Locale.ROOT);
            List<String> contains = new ArrayList<>();
            for (String id : plugin.registry().entries().keySet()) {
                if (id.startsWith(typed)) {
                    out.add(id);
                } else if (!typed.isEmpty() && id.contains(typed)) {
                    contains.add(id);
                }
                if (out.size() >= 60) {
                    break;
                }
            }
            for (String id : contains) {
                if (out.size() >= 60) {
                    break;
                }
                out.add(id);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            out.addAll(List.of("1", "16", "64"));
        }
        return out;
    }
}
