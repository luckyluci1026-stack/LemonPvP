package de.lemonpvp.easybedrock.command;

import de.lemonpvp.easybedrock.BedrockUtil;
import de.lemonpvp.easybedrock.EasyBedrock;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /easybedrock status|install|optimize
 */
public final class EasyBedrockCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("status", "install", "optimize");

    private final EasyBedrock plugin;

    public EasyBedrockCommand(EasyBedrock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("easybedrock.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "install" -> plugin.installer().installAsync(sender);
            case "optimize" -> {
                int changes = plugin.optimizer().optimize();
                if (changes < 0) {
                    plugin.msgs().send(sender, "no-geyser-folder");
                } else if (changes == 0) {
                    plugin.msgs().send(sender, "optimize-none");
                } else {
                    plugin.msgs().send(sender, "optimized", "changes", String.valueOf(changes));
                }
            }
            case "status" -> status(sender);
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    private void status(CommandSender sender) {
        plugin.msgs().send(sender, "status-header");
        boolean geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
        boolean floodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        sender.sendMessage(plugin.msgs().format("status-line", "comp", "GeyserMC",
                "state", plugin.msgs().raw(geyser ? "status-installed" : "status-missing")));
        sender.sendMessage(plugin.msgs().format("status-line", "comp", "Floodgate",
                "state", plugin.msgs().raw(floodgate ? "status-installed" : "status-missing")));
        sender.sendMessage(plugin.msgs().format("status-direct",
                "state", plugin.msgs().raw(plugin.optimizer().isDirectConnection()
                        ? "status-installed" : "status-missing")));
        sender.sendMessage(plugin.msgs().format("status-bedrock-players",
                "count", String.valueOf(BedrockUtil.countBedrock())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
