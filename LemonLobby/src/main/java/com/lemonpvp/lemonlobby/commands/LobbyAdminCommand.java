package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.model.BoosterTier;
import com.lemonpvp.lemonlobby.model.PlankTier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class LobbyAdminCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM     = MiniMessage.miniMessage();
    private static final String      PREFIX =
            "<!italic><gradient:#fffb00:#00ff00><bold>LemonLobby</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonLobby plugin;

    public LobbyAdminCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin")) {
            sender.sendMessage(MM.deserialize("<!italic><red>No permission."));
            return true;
        }
        if (args.length == 0) { sendUsage(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload"     -> handleReload(sender);
            case "upgrade"    -> handleUpgrade(sender, args);
            case "booster"    -> handleBooster(sender, args);
            case "goldenhour" -> handleGoldenHour(sender, args);
            case "hologram"   -> handleHologram(sender);
            default           -> sendUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getBoosterConfig().reload();
        plugin.getAppleTreeListener().reload();
        plugin.loadServersConfig();
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Configuration reloaded."));
    }

    private void handleUpgrade(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><gray>Usage: <white>/llobby upgrade <player> <tier>"));
            sender.sendMessage(MM.deserialize("<!italic><dark_gray>Tiers: "
                    + String.join(", ", Arrays.stream(PlankTier.values()).map(Enum::name).toList())));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<!italic><red>Player <white>" + args[1] + " <red>is not online."));
            return;
        }
        PlankTier tier = PlankTier.fromName(args[2]);
        plugin.getTreeUpgradeManager().upgrade(target.getUniqueId(), tier);
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Tree upgrade for <white>"
                + target.getName() + " <green>set to <white>" + tier.displayName + "<green>."));
        target.sendMessage(MM.deserialize(PREFIX + "<green>Your tree upgrade has been set to <white>"
                + tier.displayName + "<green>!"));
    }

    private void handleBooster(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><gray>Usage: <white>/llobby booster <player> <1-5>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<!italic><red>Player <white>" + args[1] + " <red>is not online."));
            return;
        }
        int level;
        try { level = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(MM.deserialize("<!italic><red>Invalid level (1-5).")); return; }
        BoosterTier tier = plugin.getBoosterConfig().fromLevel(level);
        if (tier == null) { sender.sendMessage(MM.deserialize("<!italic><red>Invalid level (1-5).")); return; }
        plugin.getBoosterManager().activate(target.getUniqueId(), tier);
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Booster " + tier.displayName
                + " <green>activated for <white>" + target.getName() + "<green>."));
        target.sendMessage(MM.deserialize(PREFIX + "<gold>Booster <white>" + tier.displayName
                + " <gold>has been activated for you!"));
    }

    private void handleGoldenHour(CommandSender sender, String[] args) {
        int minutes = 5;
        double mult = 2.0;
        if (args.length >= 2) { try { minutes = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {} }
        if (args.length >= 3) { try { mult = Double.parseDouble(args[2]); } catch (NumberFormatException ignored) {} }
        minutes = Math.max(1, minutes);
        if (mult <= 1.0) mult = 2.0;
        plugin.getGoldenHourManager().start(mult, minutes * 60);
        sender.sendMessage(MM.deserialize(PREFIX + "<gold>Golden Hour started: <yellow>" + mult
                + "x <gold>for <white>" + minutes + " min<gold>."));
    }

    private void handleHologram(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MM.deserialize("<!italic><red>Players only."));
            return;
        }
        plugin.getReplayHologramManager().setLocation(p);
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Recent-Plays hologram placed at your location."));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MM.deserialize(
                "<!italic><gray>/llobby <white>reload <gray>| <white>upgrade <player> <tier> <gray>| "
                        + "<white>booster <player> <1-5> <gray>| <white>goldenhour [min] [mult] <gray>| <white>hologram"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin")) return List.of();
        return switch (args.length) {
            case 1 -> filter(List.of("reload", "upgrade", "booster", "goldenhour", "hologram"), args[0]);
            case 2 -> switch (args[0].toLowerCase()) {
                case "upgrade", "booster" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                default -> List.of();
            };
            case 3 -> switch (args[0].toLowerCase()) {
                case "upgrade" -> Arrays.stream(PlankTier.values()).map(Enum::name)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).toList();
                case "booster" -> filter(List.of("1", "2", "3", "4", "5"), args[2]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private List<String> filter(List<String> opts, String prefix) {
        return opts.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
