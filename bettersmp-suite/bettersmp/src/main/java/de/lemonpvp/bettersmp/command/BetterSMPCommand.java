package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.backup.BackupSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /bettersmp status|install|update|ranks|deployconfigs|reload|backup
 */
public final class BetterSMPCommand implements TabExecutor {

    private static final List<String> SUBS =
            List.of("status", "install", "update", "ranks", "deployconfigs", "reload", "backup");

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
            case "backup" -> backup(sender, args);
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    private void backup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msgs().send(sender, "backup.usage");
            return;
        }
        String unterbefehl = args[1].toLowerCase(Locale.ROOT);
        var ziel = Bukkit.getPlayer(args[2]);
        if (ziel == null) {
            plugin.msgs().send(sender, "backup.player-offline", "spieler", args[2]);
            return;
        }
        UUID zielUuid = ziel.getUniqueId();
        String zielName = ziel.getName();
        switch (unterbefehl) {
            case "status" -> plugin.backup().status(zielUuid).thenAccept(snapshotOpt ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (snapshotOpt.isEmpty()) {
                            plugin.msgs().send(sender, "backup.none", "spieler", zielName);
                            return;
                        }
                        BackupSnapshot snapshot = snapshotOpt.get();
                        long sekundenHer = (System.currentTimeMillis() - snapshot.gespeichert()) / 1000L;
                        plugin.msgs().send(sender, "backup.status", "spieler", zielName,
                                "sekunden", String.valueOf(sekundenHer));
                    }));
            case "restore" -> plugin.backup().wiederherstellen(zielUuid).thenAccept(erfolg ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (erfolg) {
                            plugin.msgs().send(sender, "backup.restored", "spieler", zielName);
                        } else {
                            plugin.msgs().send(sender, "backup.none", "spieler", zielName);
                        }
                    }));
            default -> plugin.msgs().send(sender, "backup.usage");
        }
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
        if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
            return Stream.of("status", "restore")
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("backup")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toList());
        }
        return Stream.<String>of().collect(Collectors.toList());
    }
}
