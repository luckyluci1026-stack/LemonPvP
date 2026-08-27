package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.util.SetupDoctor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class LPracticeCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonPractice plugin;

    public LPracticeCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonpractice.admin.reload")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadSecondaryConfigs();
                plugin.getGamemodeManager().loadGamemodes();
                plugin.getArenaManager().loadAll();
                sender.sendMessage(MM.deserialize(
                    "<green>LemonPractice reloaded — kits, gamemodes, arenas, messages updated from disk."));
            }
            case "doctor" -> doctor(sender);
            default -> sender.sendMessage(MM.deserialize("<yellow>Usage: /lpractice reload|doctor"));
        }
        return true;
    }

    /** Full setup report, including the checks that passed, so "all green" is visible too. */
    private void doctor(CommandSender sender) {
        List<SetupDoctor.Finding> findings = SetupDoctor.run(plugin);
        sender.sendMessage(MM.deserialize(
                "<dark_gray><st>                              </st>"));
        sender.sendMessage(MM.deserialize("<gold><bold>LemonPractice setup</bold> <dark_gray>— <gray>"
                + plugin.getServerType() + " server"));

        int problems = 0;
        for (SetupDoctor.Finding f : findings) {
            String icon = switch (f.level()) {
                case OK -> "<green>✔";
                case WARN -> "<yellow>▲";
                case ERROR -> "<red>✖";
            };
            if (f.level() != SetupDoctor.Level.OK) problems++;
            sender.sendMessage(MM.deserialize(icon + " <white>" + escape(f.title())));
            // The detail says what to do about it — only worth the screen space when it is a problem.
            if (f.level() != SetupDoctor.Level.OK) {
                sender.sendMessage(MM.deserialize("   <gray>" + escape(f.detail())));
            } else {
                sender.sendMessage(MM.deserialize("   <dark_gray>" + escape(f.detail())));
            }
        }

        sender.sendMessage(problems == 0
                ? MM.deserialize("<green>Everything checks out.")
                : MM.deserialize("<yellow>" + problems + " problem(s) need attention."));
        sender.sendMessage(MM.deserialize(
                "<dark_gray><st>                              </st>"));
    }

    /** Findings quote config values, so neutralise anything that could look like a tag. */
    private static String escape(String raw) {
        return raw == null ? "" : MM.escapeTags(raw);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("reload", "doctor");
        return List.of();
    }
}
