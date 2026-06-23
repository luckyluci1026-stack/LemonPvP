package com.lemonpvp.lemoncore.commands.admin;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class GPlanksCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public GPlanksCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemoncore.admin.planks")) {
            sender.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<!italic><red>Verwendung: /gplanks add|remove|set|show <spieler> [menge]"));
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        if (action.equals("show")) {
            resolveUuid(targetName, uuid -> {
                if (uuid == null) {
                    run(() -> sender.sendMessage(MM.deserialize("<!italic><red>Spieler <white>" + targetName + " <red>nicht gefunden.")));
                    return;
                }
                var cached = plugin.getPlayerDataManager().getCached(uuid);
                long planks = cached != null ? cached.getPlanks() : -1;
                run(() -> sender.sendMessage(MM.deserialize("<!italic><gray>Planks von <white>" + targetName
                        + "<gray>: <#D2691E>▬ <white>" + (planks >= 0 ? TextUtil.formatCoins(planks) : "nicht geladen"))));
            });
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MM.deserialize("<!italic><red>Verwendung: /gplanks " + action + " <spieler> <menge>"));
            return true;
        }
        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(MM.deserialize("<!italic><red>Ungültige Zahl."));
            return true;
        }
        final long amt = amount;

        resolveUuid(targetName, uuid -> {
            if (uuid == null) {
                run(() -> sender.sendMessage(MM.deserialize("<!italic><red>Spieler <white>" + targetName + " <red>nicht gefunden.")));
                return;
            }
            switch (action) {
                case "add" -> plugin.getPlayerDataManager().addPlanks(uuid, amt)
                        .thenRun(() -> run(() -> {
                            sender.sendMessage(MM.deserialize("<!italic><green>+<white>" + amt + " <#D2691E>▬ <green>Planks zu <white>" + targetName + " <green>hinzugefügt."));
                            Player t = Bukkit.getPlayer(uuid);
                            if (t != null) t.sendMessage(MM.deserialize(
                                    "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray>"
                                    + " <green>Du hast <white>" + TextUtil.formatCoins(amt) + " <#D2691E>▬ <green>Planks erhalten!"));
                        }));
                case "remove" -> plugin.getPlayerDataManager().removePlanks(uuid, amt)
                        .thenRun(() -> run(() ->
                            sender.sendMessage(MM.deserialize("<!italic><red>-<white>" + amt + " <red>▬ Planks von <white>" + targetName + " <red>abgezogen."))));
                case "set" -> {
                    var cached = plugin.getPlayerDataManager().getCached(uuid);
                    long current = cached != null ? cached.getPlanks() : 0;
                    long diff = amt - current;
                    plugin.getPlayerDataManager().addPlanks(uuid, diff)
                            .thenRun(() -> run(() ->
                                sender.sendMessage(MM.deserialize("<!italic><green>Planks von <white>" + targetName + " <green>auf <white>" + TextUtil.formatCoins(amt) + " <green>gesetzt."))));
                }
                default -> run(() -> sender.sendMessage(MM.deserialize("<!italic><red>Unbekannte Aktion. Nutze add|remove|set|show")));
            }
        });
        return true;
    }

    private void resolveUuid(String name, Consumer<UUID> callback) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) { callback.accept(online.getUniqueId()); return; }
        plugin.getPlayerDataManager().findUUIDByName(name).thenAccept(callback);
    }

    private void run(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }
}
