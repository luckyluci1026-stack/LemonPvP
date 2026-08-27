package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerTracker;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GCheckCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;

    public GCheckCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.check")) {
            sender.sendMessage(MM.deserialize("<red>No permission.</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /gcheck <player></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<red>Player not found: " + args[0] + "</red>"));
            return true;
        }

        PlayerTracker tracker = plugin.getPlayerTracker();

        // Live values
        int ping       = target.getPing();
        int cps        = tracker.getCps(target.getUniqueId());
        double reach   = tracker.getLastReach(target.getUniqueId());
        long onlineMs  = tracker.getOnlineMillis(target.getUniqueId());
        String server  = plugin.getConfig().getString("server-name", "unknown");
        String world   = target.getWorld().getName();
        int x          = target.getLocation().getBlockX();
        int y          = target.getLocation().getBlockY();
        int z          = target.getLocation().getBlockZ();
        String gamemode = target.getGameMode().name();
        double health  = Math.round(target.getHealth() * 10.0) / 10.0;
        int food       = target.getFoodLevel();
        boolean flying = target.isFlying();
        float speed    = Math.round(target.getWalkSpeed() * 100.0f) / 100.0f;

        long totalSecs = onlineMs / 1000;
        String onlineStr = String.format("%02d:%02d:%02d",
                totalSecs / 3600, (totalSecs % 3600) / 60, totalSecs % 60);
        String reachStr = reach == 0.0 ? "N/A" : reach + " blocks";

        sender.sendMessage(MM.deserialize(
                "<gray>┌─ <gradient:#fffb00:#00ff00>LemonPvP</gradient><gray> — Player Info ─┐</gray>"));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Player:     <yellow>" + target.getName()
                        + " <gray>(" + target.getUniqueId() + ")"));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Ping:       <yellow>" + ping + "ms"));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>CPS:        <yellow>" + cps));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Reach:      <yellow>" + reachStr));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Location:   <yellow>" + world
                        + " X:" + x + " Y:" + y + " Z:" + z));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Gamemode:   <yellow>" + gamemode));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Server:     <yellow>" + server));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Health:     <yellow>" + health + "❤"));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Food:       <yellow>" + food));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Fly:        <yellow>" + (flying ? "Yes" : "No")));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Speed:      <yellow>" + speed));
        sender.sendMessage(MM.deserialize(
                "<gray>│ <white>Online for: <yellow>" + onlineStr));
        sender.sendMessage(MM.deserialize(
                "<gray>└──────────────────────────────────────┘"));

        return true;
    }
}
