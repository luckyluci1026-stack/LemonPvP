package de.lemonpvp.betterrtp.command;

import de.lemonpvp.betterrtp.BetterRTP;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /rtp [Welt|Spieler] - Zufalls-Teleport.
 */
public final class RTPCommand implements TabExecutor {

    private final BetterRTP plugin;

    public RTPCommand(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("betterrtp.use")) {
            plugin.msgs().send(player, "no-permission");
            return true;
        }

        World world = player.getWorld();
        if (args.length >= 1) {
            // Andere Welt (nur mit Recht) oder anderen Spieler teleportieren (Admin)
            World named = plugin.getServer().getWorld(args[0]);
            if (named != null) {
                if (!player.hasPermission("betterrtp.world")) {
                    plugin.msgs().send(player, "no-permission");
                    return true;
                }
                world = named;
            } else {
                Player target = plugin.getServer().getPlayerExact(args[0]);
                if (target != null && player.hasPermission("betterrtp.admin")) {
                    plugin.rtp().request(target, target.getWorld());
                    return true;
                }
                plugin.msgs().send(player, "world-not-found", "world", args[0]);
                return true;
            }
        } else {
            String def = plugin.getConfig().getString("settings.default-world", "");
            if (def != null && !def.isBlank()) {
                World d = plugin.getServer().getWorld(def);
                if (d != null) {
                    world = d;
                }
            }
        }

        plugin.rtp().request(player, world);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("betterrtp.world")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(world.getName());
                }
            }
        }
        return out;
    }
}
