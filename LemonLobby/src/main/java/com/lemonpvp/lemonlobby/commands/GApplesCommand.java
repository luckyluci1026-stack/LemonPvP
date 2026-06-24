package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.util.EconomyBridge;
import com.lemonpvp.lemonlobby.util.FormatUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GApplesCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonLobby plugin;

    public GApplesCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin.apples")) {
            sender.sendMessage(MM.deserialize("<!italic><red>Keine Berechtigung."));
            return true;
        }
        if (EconomyBridge.core() == null) {
            sender.sendMessage(MM.deserialize("<!italic><red>Economy-System nicht verfügbar."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<!italic><red>Verwendung: /gapples add|remove|set|show <spieler> [menge]"));
            return true;
        }

        String action     = args[0].toLowerCase();
        String targetName = args[1];

        if (action.equals("show")) {
            resolveUuid(targetName, uuid -> {
                if (uuid == null) { run(() -> notFound(sender, targetName)); return; }
                PlayerData pd = EconomyBridge.cached(uuid);
                long apples = pd != null ? pd.getApples() : -1;
                run(() -> sender.sendMessage(MM.deserialize("<!italic><gray>Äpfel von <white>" + targetName
                        + "<gray>: <green>✿ <white>"
                        + (apples >= 0 ? FormatUtil.formatAmount(apples) : "nicht geladen"))));
            });
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><red>Verwendung: /gapples " + action + " <spieler> <menge>"));
            return true;
        }
        long amount;
        try { amount = Long.parseLong(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(MM.deserialize("<!italic><red>Ungültige Zahl.")); return true; }
        final long amt = amount;

        resolveUuid(targetName, uuid -> {
            if (uuid == null) { run(() -> notFound(sender, targetName)); return; }
            switch (action) {
                case "add" -> EconomyBridge.addApples(uuid, amt)
                        .thenRun(() -> run(() -> {
                            sender.sendMessage(MM.deserialize("<!italic><green>+<white>" + amt
                                    + " <green>✿ Äpfel zu <white>" + targetName + " <green>hinzugefügt."));
                            Player t = Bukkit.getPlayer(uuid);
                            if (t != null) t.sendMessage(MM.deserialize(
                                    "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray>"
                                    + " <green>Du hast <white>" + FormatUtil.formatAmount(amt) + " <green>✿ Äpfel erhalten!"));
                        }));
                case "remove" -> EconomyBridge.removeApples(uuid, amt)
                        .thenRun(() -> run(() ->
                            sender.sendMessage(MM.deserialize("<!italic><red>-<white>" + amt
                                    + " <red>✿ Äpfel von <white>" + targetName + " <red>abgezogen."))));
                case "set" -> {
                    PlayerData pd = EconomyBridge.cached(uuid);
                    long current  = pd != null ? pd.getApples() : 0;
                    EconomyBridge.addApples(uuid, amt - current)
                            .thenRun(() -> run(() ->
                                sender.sendMessage(MM.deserialize("<!italic><green>Äpfel von <white>"
                                        + targetName + " <green>auf <white>"
                                        + FormatUtil.formatAmount(amt) + " <green>gesetzt."))));
                }
                default -> run(() -> sender.sendMessage(MM.deserialize("<!italic><red>Unbekannte Aktion: add|remove|set|show")));
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lemonlobby.admin.apples")) return List.of();
        return switch (args.length) {
            case 1 -> List.of("add", "remove", "set", "show").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
            case 2 -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            default -> List.of();
        };
    }

    private void resolveUuid(String name, Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        EconomyBridge.findUUID(name).thenAccept(callback);
    }

    private void run(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }

    private void notFound(CommandSender s, String name) {
        s.sendMessage(MM.deserialize("<!italic><red>Spieler <white>" + name + " <red>nicht gefunden."));
    }
}
