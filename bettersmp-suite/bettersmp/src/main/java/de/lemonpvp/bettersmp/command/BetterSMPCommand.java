package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /bettersmp status|install|update|ranks|deployconfigs|reload
 */
public final class BetterSMPCommand implements TabExecutor {

    private static final List<String> SUBS =
            List.of("status", "install", "update", "ranks", "deployconfigs", "reload");

    private final BetterSMP plugin;

    public BetterSMPCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bettersmp.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender);
            case "install", "update" -> plugin.installer().installAsync(sender);
            case "ranks" -> plugin.setupRanks(sender);
            case "deployconfigs" -> {
                plugin.configDeployer().deployAll(sender);
                plugin.configDeployer().patchServerProperties(sender);
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.reloadModules();
                plugin.msgs().send(sender, "reloaded");
            }
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    private void status(CommandSender sender) {
        plugin.msgs().send(sender, "status.header");
        for (String name : List.of("Essentials", "LuckPerms", "Vault", "PlaceholderAPI", "TAB")) {
            Plugin dep = Bukkit.getPluginManager().getPlugin(name);
            String state = dep != null
                    ? plugin.msgs().raw("status.installed").replace("%version%",
                            dep.getPluginMeta().getVersion())
                    : plugin.msgs().raw("status.missing");
            sender.sendMessage(plugin.msgs().format("status.line",
                    "plugin", name, "status", state));
        }
        long inCombat = Bukkit.getOnlinePlayers().stream()
                .filter(p -> plugin.combat().isTagged(p.getUniqueId())).count();
        sender.sendMessage(plugin.msgs().format("status.combat-line",
                "count", String.valueOf(inCombat)));
        sender.sendMessage(plugin.msgs().format("status.db-line",
                "type", plugin.database().typeName()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Stream.<String>of().collect(Collectors.toList());
    }
}
