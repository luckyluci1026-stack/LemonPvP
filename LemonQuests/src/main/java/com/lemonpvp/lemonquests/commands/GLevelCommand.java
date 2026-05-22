package com.lemonpvp.lemonquests.commands;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.PlayerData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GLevelCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonQuests plugin;

    public GLevelCommand(LemonQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonquests.admin.level")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        String playerName = args[1];

        Player onlineTarget = Bukkit.getPlayer(playerName);
        UUID targetUuid = resolveUuid(playerName, onlineTarget);

        if (targetUuid == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + playerName + "</red>"));
            return true;
        }

        switch (sub) {
            case "set" -> {
                if (args.length < 3) { sendHelp(sender, label); return true; }
                int amount = parsePositive(sender, args[2]);
                if (amount < 0) return true;

                plugin.getPlayerDataManager().setLevel(targetUuid, amount)
                        .thenRun(() -> sendLevelFeedback(sender, playerName, targetUuid));
            }
            case "add" -> {
                if (args.length < 3) { sendHelp(sender, label); return true; }
                int amount = parsePositive(sender, args[2]);
                if (amount < 0) return true;

                plugin.getPlayerDataManager().addLevel(targetUuid, amount)
                        .thenRun(() -> sendLevelFeedback(sender, playerName, targetUuid));
            }
            case "remove" -> {
                if (args.length < 3) { sendHelp(sender, label); return true; }
                int amount = parsePositive(sender, args[2]);
                if (amount < 0) return true;

                plugin.getPlayerDataManager().subtractLevel(targetUuid, amount)
                        .thenRun(() -> sendLevelFeedback(sender, playerName, targetUuid));
            }
            case "reset" -> {
                plugin.getPlayerDataManager().resetPlayer(targetUuid)
                        .thenRun(() -> sendLevelFeedback(sender, playerName, targetUuid));
            }
            default -> sendHelp(sender, label);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendLevelFeedback(CommandSender sender, String playerName, UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerData pd = plugin.getPlayerDataManager().getPlayer(uuid);
            int newLevel = (pd != null) ? pd.getLevel() : 1;
            sender.sendMessage(MM.deserialize(
                    "<green>Level updated for <yellow>" + playerName + "</yellow>: "
                            + "Level <yellow>" + newLevel + "</yellow></green>"));
        });
    }

    private int parsePositive(CommandSender sender, String raw) {
        try {
            int v = Integer.parseInt(raw);
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            sender.sendMessage(MM.deserialize("<red>Invalid amount: " + raw + " (must be a positive integer)</red>"));
            return -1;
        }
    }

    private UUID resolveUuid(String name, Player online) {
        if (online != null) return online.getUniqueId();
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(MM.deserialize("<gold>/" + label + " set <player> <level></gold>"));
        sender.sendMessage(MM.deserialize("<gold>/" + label + " add <player> <amount></gold>"));
        sender.sendMessage(MM.deserialize("<gold>/" + label + " remove <player> <amount></gold>"));
        sender.sendMessage(MM.deserialize("<gold>/" + label + " reset <player></gold>"));
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set", "add", "remove", "reset"), args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return filter(names, args[1]);
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (!sub.equals("reset")) {
                return List.of("<amount>");
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lp = prefix.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lp)) result.add(s);
        }
        return result;
    }
}
