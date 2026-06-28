package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CoderlCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public CoderlCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.coderl")) {
            sender.sendMessage(MM.deserialize("<!italic><red>No permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                Set<String> names = plugin.getScriptManager().getNames();
                if (names.isEmpty()) {
                    sender.sendMessage(MM.deserialize("<!italic><gray>No scripts loaded."));
                } else {
                    sender.sendMessage(MM.deserialize(
                            "<!italic><gradient:#fffb00:#00ff00>Loaded Scripts <dark_gray>(" + names.size() + "):"));
                    names.forEach(n -> sender.sendMessage(MM.deserialize("<!italic>  <white>» <green>" + n)));
                }
            }
            case "load" -> {
                if (args.length < 2) { sender.sendMessage(MM.deserialize("<!italic><red>Usage: /coderl load <name>")); return true; }
                String name = args[1];
                sender.sendMessage(MM.deserialize("<!italic><gray>Compiling <white>" + name + "<gray>..."));
                String err = plugin.getScriptManager().load(name);
                if (err == null) {
                    sender.sendMessage(MM.deserialize("<!italic><green>✔ Script <white>" + name + " <green>loaded."));
                } else {
                    sender.sendMessage(MM.deserialize("<!italic><red>✘ Error loading <white>" + name + "<red>:"));
                    for (String line : err.split("\n"))
                        sender.sendMessage(MM.deserialize("<!italic>  <red>" + line));
                }
            }
            case "unload" -> {
                if (args.length < 2) { sender.sendMessage(MM.deserialize("<!italic><red>Usage: /coderl unload <name>")); return true; }
                String name = args[1];
                boolean ok = plugin.getScriptManager().unload(name);
                sender.sendMessage(ok
                        ? MM.deserialize("<!italic><green>✔ Script <white>" + name + " <green>unloaded.")
                        : MM.deserialize("<!italic><red>Script <white>" + name + " <red>not loaded."));
            }
            case "reload" -> {
                if (args.length < 2) {
                    // Reload all
                    sender.sendMessage(MM.deserialize("<!italic><gray>Reloading all scripts..."));
                    Map<String, String> errors = plugin.getScriptManager().loadAll();
                    if (errors.isEmpty()) {
                        sender.sendMessage(MM.deserialize("<!italic><green>✔ All scripts reloaded."));
                    } else {
                        errors.forEach((n, e) -> {
                            sender.sendMessage(MM.deserialize("<!italic><red>✘ " + n + ":"));
                            for (String line : e.split("\n"))
                                sender.sendMessage(MM.deserialize("<!italic>  <red>" + line));
                        });
                    }
                } else {
                    String name = args[1];
                    sender.sendMessage(MM.deserialize("<!italic><gray>Reloading <white>" + name + " <gray>..."));
                    String err = plugin.getScriptManager().reload(name);
                    if (err == null) {
                        sender.sendMessage(MM.deserialize("<!italic><green>✔ Script <white>" + name + " <green>reloaded."));
                    } else {
                        sender.sendMessage(MM.deserialize("<!italic><red>✘ Error:"));
                        for (String line : err.split("\n"))
                            sender.sendMessage(MM.deserialize("<!italic>  <red>" + line));
                    }
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.coderl")) return List.of();
        if (args.length == 1) return filterStart(List.of("list", "load", "reload", "unload"), args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("unload") || sub.equals("reload"))
                return filterStart(plugin.getScriptManager().getNames().stream().toList(), args[1]);
            if (sub.equals("load"))
                return filterStart(availableScripts(), args[1]);
        }
        return List.of();
    }

    private List<String> availableScripts() {
        java.io.File dir = new java.io.File(plugin.getDataFolder(), "scripts");
        java.io.File[] files = dir.listFiles(f -> f.getName().endsWith(".java"));
        if (files == null) return List.of();
        return java.util.Arrays.stream(files).map(f -> f.getName().replace(".java", "")).toList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>CodeRL — Java Hot-Reload"));
        sender.sendMessage(MM.deserialize("<!italic><gray>/coderl list <dark_gray>— <white>Show loaded scripts"));
        sender.sendMessage(MM.deserialize("<!italic><gray>/coderl load <name> <dark_gray>— <white>Load script"));
        sender.sendMessage(MM.deserialize("<!italic><gray>/coderl unload <name> <dark_gray>— <white>Unload script"));
        sender.sendMessage(MM.deserialize("<!italic><gray>/coderl reload [name] <dark_gray>— <white>Reload script (all if no name)"));
        sender.sendMessage(MM.deserialize("<!italic><dark_gray>Scripts are located in: plugins/LemonCore/scripts/"));
    }

    private List<String> filterStart(List<String> options, String prefix) {
        return options.stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
