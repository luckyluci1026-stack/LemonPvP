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
            sender.sendMessage(MM.deserialize("<!italic><red>Keine Berechtigung."));
            return true;
        }
        if (args.length == 0) { sendUsage(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload"  -> handleReload(sender);
            case "upgrade" -> handleUpgrade(sender, args);
            case "booster" -> handleBooster(sender, args);
            default        -> sendUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getBoosterConfig().reload();
        plugin.getAppleTreeListener().reload();
        plugin.loadServersConfig();
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Konfiguration neu geladen."));
    }

    private void handleUpgrade(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><gray>Verwendung: <white>/llobby upgrade <spieler> <tier>"));
            sender.sendMessage(MM.deserialize("<!italic><dark_gray>Tiers: "
                    + String.join(", ", Arrays.stream(PlankTier.values()).map(Enum::name).toList())));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<!italic><red>Spieler <white>" + args[1] + " <red>nicht online."));
            return;
        }
        PlankTier tier = PlankTier.fromName(args[2]);
        plugin.getTreeUpgradeManager().upgrade(target.getUniqueId(), tier);
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Baum-Upgrade von <white>"
                + target.getName() + " <green>auf <white>" + tier.displayName + " <green>gesetzt."));
        target.sendMessage(MM.deserialize(PREFIX + "<green>Dein Baum-Upgrade wurde auf <white>"
                + tier.displayName + " <green>gesetzt!"));
    }

    private void handleBooster(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><gray>Verwendung: <white>/llobby booster <spieler> <1-5>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MM.deserialize("<!italic><red>Spieler <white>" + args[1] + " <red>nicht online."));
            return;
        }
        int level;
        try { level = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(MM.deserialize("<!italic><red>Ungültige Stufe (1-5).")); return; }
        BoosterTier tier = plugin.getBoosterConfig().fromLevel(level);
        if (tier == null) { sender.sendMessage(MM.deserialize("<!italic><red>Ungültige Stufe (1-5).")); return; }
        plugin.getBoosterManager().activate(target.getUniqueId(), tier);
        sender.sendMessage(MM.deserialize(PREFIX + "<green>Verstärker " + tier.displayName
                + " <green>für <white>" + target.getName() + " <green>aktiviert."));
        target.sendMessage(MM.deserialize(PREFIX + "<gold>Verstärker <white>" + tier.displayName
                + " <gold>wurde für dich aktiviert!"));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MM.deserialize(
                "<!italic><gray>/llobby <white>reload <gray>| <white>upgrade <spieler> <tier> <gray>| <white>booster <spieler> <1-5>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin")) return List.of();
        return switch (args.length) {
            case 1 -> filter(List.of("reload", "upgrade", "booster"), args[0]);
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
