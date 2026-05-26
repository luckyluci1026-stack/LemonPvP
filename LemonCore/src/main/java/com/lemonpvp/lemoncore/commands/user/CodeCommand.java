package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.CodeManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CodeCommand implements CommandExecutor {

    private final LemonCore plugin;
    private final LuckPerms lp;

    public CodeCommand(LemonCore plugin, LuckPerms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.code")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/code <code>"));
            return true;
        }

        String code = args[0];
        plugin.getCodeManager().redeemCode(code, player.getUniqueId()).thenAccept(result -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result.result) {
                    case SUCCESS -> {
                        applyReward(player, result);
                        player.sendMessage(plugin.getMessagesManager().get("code.success-header"));
                        player.sendMessage(plugin.getMessagesManager().get("code.success-reward",
                                "reward", formatReward(result)));
                        player.sendMessage(plugin.getMessagesManager().get("code.success-footer"));
                        player.playSound(player.getLocation(),
                                org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                    case ALREADY_USED -> {
                        player.playSound(player.getLocation(),
                                org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(plugin.getMessagesManager().get("code.already-redeemed"));
                    }
                    case EXPIRED -> {
                        player.playSound(player.getLocation(),
                                org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(plugin.getMessagesManager().get("code.expired"));
                    }
                    default -> {
                        player.playSound(player.getLocation(),
                                org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(plugin.getMessagesManager().get("code.invalid"));
                    }
                }
            });
        });
        return true;
    }

    private void applyReward(Player player, CodeManager.CodeData data) {
        switch (data.rewardType) {
            case "coins" -> {
                try {
                    long amount = Long.parseLong(data.rewardValue);
                    plugin.getPlayerDataManager().addCoins(player.getUniqueId(), amount, "code:" + data.code, null);
                } catch (NumberFormatException ignored) {}
            }
            case "rank" -> {
                lp.getUserManager().loadUser(player.getUniqueId()).thenAccept(user -> {
                    user.data().add(InheritanceNode.builder(data.rewardValue).build());
                    lp.getUserManager().saveUser(user);
                });
            }
            case "killeffect" -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        org.bukkit.Bukkit.getPluginManager().callEvent(
                                new com.lemonpvp.lemoncore.events.KillEffectRewardEvent(
                                        player.getUniqueId(), data.rewardValue)));
            }
        }
    }

    private String formatReward(CodeManager.CodeData data) {
        return switch (data.rewardType) {
            case "coins" -> data.rewardValue + " Coins";
            case "rank" -> "Rank: " + data.rewardValue;
            case "killeffect" -> "Kill Effect: " + data.rewardValue;
            default -> data.rewardValue;
        };
    }
}
