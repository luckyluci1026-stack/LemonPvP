package com.lemonpvp.lemoncore.lemonlang.command;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.lemonlang.LemonLangError;
import com.lemonpvp.lemoncore.lemonlang.LemonLangManager;
import com.lemonpvp.lemoncore.lemonlang.LemonLangScript;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for the {@code /lemonlang} command (aliases: {@code /ll}, {@code /lang}).
 *
 * <p>Subcommands: list, reload [name], error {@code <name>}, info.
 */
public final class LemonLangCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "lemoncore.admin.lemonlang";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** LemonPvP gradient prefix used by all LemonLang admin messages. */
    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray> <gray><!italic>";

    private final LemonCore plugin;

    public LemonLangCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(parse(PREFIX + "<red>Keine Berechtigung."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "list"   -> cmdList(sender);
            case "reload" -> cmdReload(sender, args);
            case "error"  -> cmdError(sender, args);
            case "info"   -> cmdInfo(sender);
            default       -> { sendHelp(sender, label); yield true; }
        };
    }

    // ---- Subcommands ----------------------------------------------------------

    private boolean cmdList(CommandSender sender) {
        LemonLangManager mgr = plugin.getLemonLangManager();
        if (mgr == null) { sender.sendMessage(parse(PREFIX + "<red>LemonLang nicht initialisiert.")); return true; }

        Map<String, LemonLangScript> scripts = mgr.getScripts();
        if (scripts.isEmpty()) {
            sender.sendMessage(parse(PREFIX + "Keine Skripte geladen."));
            return true;
        }

        sender.sendMessage(parse(PREFIX + "<yellow>Geladene Skripte <dark_gray>(" + scripts.size() + "):"));
        for (LemonLangScript script : scripts.values()) {
            long sizeKb = script.file().length() / 1024;
            String status = script.hasErrors()
                    ? "<red>[Fehler]"
                    : (script.loaded() ? "<green>[OK]" : "<yellow>[ausstehend]");
            sender.sendMessage(parse("  <white>" + script.name() + ".lemon"
                    + " <dark_gray>(" + sizeKb + " KB) "
                    + status
                    + " <dark_gray>- " + script.triggerCount() + "T "
                    + script.itemCount() + "I "
                    + script.commandCount() + "C "
                    + script.guiCount() + "G"));
        }
        return true;
    }

    private boolean cmdReload(CommandSender sender, String[] args) {
        LemonLangManager mgr = plugin.getLemonLangManager();
        if (mgr == null) { sender.sendMessage(parse(PREFIX + "<red>LemonLang nicht initialisiert.")); return true; }

        if (args.length >= 2) {
            String scriptName = args[1];
            sender.sendMessage(parse(PREFIX + "Lade Skript <yellow>" + scriptName + "</yellow> neu..."));
            mgr.reloadScript(scriptName);
            boolean hasErr = mgr.getLastError(scriptName).isPresent();
            if (hasErr) {
                sender.sendMessage(parse(PREFIX + "<red>Fehler beim Laden. Nutze <yellow>/lemonlang error " + scriptName + "</yellow> fuer Details."));
            } else {
                sender.sendMessage(parse(PREFIX + "<green>Skript <yellow>" + scriptName + "</yellow> erfolgreich neu geladen."));
            }
        } else {
            sender.sendMessage(parse(PREFIX + "Alle Skripte werden neu geladen..."));
            mgr.reload();
            sender.sendMessage(parse(PREFIX + "<green>Alle Skripte neu geladen. "
                    + mgr.getPrograms().size() + " Skript(e) aktiv."));
        }
        return true;
    }

    private boolean cmdError(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(parse(PREFIX + "<red>Verwendung: /lemonlang error <name>"));
            return true;
        }
        LemonLangManager mgr = plugin.getLemonLangManager();
        if (mgr == null) { sender.sendMessage(parse(PREFIX + "<red>LemonLang nicht initialisiert.")); return true; }

        String name = args[1];
        Optional<LemonLangError> errOpt = mgr.getLastError(name);
        if (errOpt.isEmpty()) {
            sender.sendMessage(parse(PREFIX + "<green>Kein Fehler fuer Skript <yellow>" + name + "</yellow>."));
        } else {
            LemonLangError err = errOpt.get();
            sender.sendMessage(err.formatForChat());
        }
        return true;
    }

    private boolean cmdInfo(CommandSender sender) {
        LemonLangManager mgr = plugin.getLemonLangManager();
        if (mgr == null) { sender.sendMessage(parse(PREFIX + "<red>LemonLang nicht initialisiert.")); return true; }

        sender.sendMessage(parse(PREFIX + "<yellow>LemonLang v3 Info:"));
        sender.sendMessage(parse("  <white>Skripte: <aqua>" + mgr.getScripts().size()));
        sender.sendMessage(parse("  <white>Items: <aqua>" + mgr.getItemRegistry().size()));
        sender.sendMessage(parse("  <white>GUIs: <aqua>" + mgr.getGuiRegistry().size()));

        long totalErrors = mgr.getScripts().values().stream()
                .filter(LemonLangScript::hasErrors).count();
        sender.sendMessage(parse("  <white>Skripte mit Fehlern: <" +
                (totalErrors > 0 ? "red" : "green") + ">" + totalErrors));
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(parse(PREFIX + "<yellow>LemonLang Befehle:"));
        sender.sendMessage(parse("  <white>/" + label + " list"));
        sender.sendMessage(parse("  <white>/" + label + " reload [name]"));
        sender.sendMessage(parse("  <white>/" + label + " error <name>"));
        sender.sendMessage(parse("  <white>/" + label + " info"));
    }

    // ---- Tab Completion -------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) return List.of();

        if (args.length == 1) {
            return filterStart(List.of("list", "reload", "error", "info"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("error"))) {
            LemonLangManager mgr = plugin.getLemonLangManager();
            if (mgr == null) return List.of();
            return mgr.getScripts().keySet().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // ---- Helpers ---------------------------------------------------------------

    private static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    private static List<String> filterStart(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.startsWith(lower))
                .collect(Collectors.toList());
    }
}
