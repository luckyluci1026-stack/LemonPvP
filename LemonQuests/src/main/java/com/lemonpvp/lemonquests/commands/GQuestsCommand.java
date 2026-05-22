package com.lemonpvp.lemonquests.commands;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.model.PlayerData;
import com.lemonpvp.lemonquests.model.QuestDefinition;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GQuestsCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonQuests plugin;

    public GQuestsCommand(LemonQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> handleReload(sender);
            case "give"   -> handleGive(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "info"   -> handleInfo(sender, args);
            default       -> sendHelp(sender);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Subcommands
    // -------------------------------------------------------------------------

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lemonquests.admin.reload")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return;
        }

        plugin.reloadConfig();
        plugin.getQuestManager().reloadQuests();
        plugin.getPlayerDataManager().loadRankLevels();

        sender.sendMessage(MM.deserialize("<green>LemonQuests config and quests reloaded.</green>"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lemonquests.admin.give")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<red>Usage: /gquests give <player> <questId></red>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + args[1] + "</red>"));
            return;
        }

        String questId = args[2];
        QuestDefinition def = plugin.getQuestManager().getQuest(questId);
        if (def == null) {
            sender.sendMessage(MM.deserialize("<red>Unknown quest: " + questId + "</red>"));
            return;
        }

        plugin.getQuestManager().ensureInTodaysQuests(questId);
        sender.sendMessage(MM.deserialize(
                "<green>Quest <yellow>" + def.getDisplayName()
                        + "</yellow> added to today's quest pool and is now available for <yellow>"
                        + target.getName() + "</yellow>.</green>"));
    }

    private void handleComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lemonquests.admin.complete")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<red>Usage: /gquests complete <player> <questId></red>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + args[1] + "</red>"));
            return;
        }

        String questId = args[2];
        QuestDefinition def = plugin.getQuestManager().getQuest(questId);
        if (def == null) {
            sender.sendMessage(MM.deserialize("<red>Unknown quest: " + questId + "</red>"));
            return;
        }

        if (plugin.getQuestManager().isCompleted(target.getUniqueId(), questId)) {
            sender.sendMessage(MM.deserialize(
                    "<yellow>" + target.getName() + " has already completed quest '"
                            + def.getDisplayName() + "'.</yellow>"));
            return;
        }

        // Ensure it's in today's pool so completeQuest can find it
        plugin.getQuestManager().ensureInTodaysQuests(questId);
        plugin.getQuestManager().completeQuest(target.getUniqueId(), questId);

        sender.sendMessage(MM.deserialize(
                "<green>Marked quest <yellow>" + def.getDisplayName()
                        + "</yellow> as completed for <yellow>" + target.getName() + "</yellow>.</green>"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lemonquests.admin.info")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<red>Usage: /gquests info <player></red>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + args[1] + "</red>"));
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getPlayer(target.getUniqueId());
        int level   = (pd != null) ? pd.getLevel() : 0;
        int xp      = (pd != null) ? pd.getTotalXp() : 0;
        int completed = plugin.getQuestManager().getCompletedCountToday(target.getUniqueId());

        sender.sendMessage(MM.deserialize("<gold>=== Quest Info: " + target.getName() + " ===</gold>"));
        sender.sendMessage(MM.deserialize("<white>Level: <yellow>" + level + "</yellow></white>"));
        sender.sendMessage(MM.deserialize("<white>Total XP: <yellow>" + xp + "</yellow></white>"));
        sender.sendMessage(MM.deserialize(
                "<white>Quests completed today: <yellow>" + completed + "</yellow></white>"));

        // List today's quests and their completion status
        List<QuestDefinition> todays = plugin.getQuestManager().getTodaysQuests();
        if (todays.isEmpty()) {
            sender.sendMessage(MM.deserialize("<gray>No quests selected for today.</gray>"));
        } else {
            for (QuestDefinition def : todays) {
                boolean done = plugin.getQuestManager()
                        .isCompleted(target.getUniqueId(), def.getId());
                int progress = plugin.getQuestManager()
                        .getProgressToday(target.getUniqueId(), def.getId());
                String status = done ? "<green>✔</green>" : "<gray>" + progress + "/" + def.getTarget() + "</gray>";
                sender.sendMessage(MM.deserialize(
                        " <white>- " + def.getDisplayName() + ": " + status + "</white>"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Help
    // -------------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize("<gold>/gquests reload</gold> <gray>— Reload config & quests</gray>"));
        sender.sendMessage(MM.deserialize("<gold>/gquests give <player> <questId></gold> <gray>— Add quest to today's pool</gray>"));
        sender.sendMessage(MM.deserialize("<gold>/gquests complete <player> <questId></gold> <gray>— Force-complete a quest</gray>"));
        sender.sendMessage(MM.deserialize("<gold>/gquests info <player></gold> <gray>— Show player quest info</gray>"));
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "give", "complete", "info"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("complete") || sub.equals("info")) {
                List<String> names = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                return filter(names, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("complete")) {
                return filter(new ArrayList<>(plugin.getQuestManager().getAllQuests().keySet()), args[2]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lowerPrefix)) result.add(s);
        }
        return result;
    }
}
