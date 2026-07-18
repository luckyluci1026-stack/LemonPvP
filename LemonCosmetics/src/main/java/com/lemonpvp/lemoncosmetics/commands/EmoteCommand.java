package com.lemonpvp.lemoncosmetics.commands;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /emote}:
 * <ul>
 *   <li>{@code /emote} or {@code /emote list} — list available emotes</li>
 *   <li>{@code /emote <name>} — play it above your head</li>
 *   <li>{@code /emote stop} — stop your current emote</li>
 * </ul>
 * Emotes are gif/png files in {@code plugins/LemonCosmetics/emotes/} — drop a
 * file in, and it's playable. No resource pack.
 */
public class EmoteCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#ffe259:#ffa751><bold>Emote</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonCosmetics plugin;

    public EmoteCommand(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("lemoncosmetics.emote")) {
            msg(player, "<red>You don't have permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listEmotes(player);
            return true;
        }
        var dances = plugin.getDanceEmoteManager();
        if (args[0].equalsIgnoreCase("stop")) {
            plugin.getEmoteManager().stop(player.getUniqueId());
            if (dances != null) dances.stop(player.getUniqueId());
            msg(player, "<gray>Emote stopped.");
            return true;
        }

        // Fortnite-style dances take priority over image emotes of the same name.
        if (dances != null && dances.isDance(args[0])) {
            dances.play(player, args[0]);
            return true;
        }

        long left = plugin.getEmoteManager().cooldownLeft(player.getUniqueId());
        if (left > 0) {
            msg(player, "<red>Please wait <yellow>" + left + "s <red>before the next emote.");
            return true;
        }

        String name = args[0];
        if (plugin.getEmoteManager().play(player, name)) {
            msg(player, "<green>Playing <yellow>" + name + "<green>!");
        } else {
            msg(player, "<red>Emote <yellow>" + name + " <red>not found or unreadable. Try <white>/emote list<red>.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("lemoncosmetics.emote")) return List.of();
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String sub : List.of("list", "stop")) if (sub.startsWith(prefix)) out.add(sub);
        var dances = plugin.getDanceEmoteManager();
        if (dances != null) {
            for (String d : dances.danceIds()) if (d.startsWith(prefix)) out.add(d);
        }
        for (String e : plugin.getEmoteManager().listEmotes()) {
            if (e.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(e);
        }
        return out;
    }

    private void listEmotes(Player player) {
        var dances = plugin.getDanceEmoteManager();
        if (dances != null) {
            StringBuilder db = new StringBuilder();
            for (String d : dances.danceIds()) {
                db.append("<click:run_command:'/emote ").append(d).append("'>")
                  .append("<hover:show_text:'<green>Click to dance'><gold>").append(d)
                  .append("</gold></hover></click>  ");
            }
            msg(player, PREFIX + "<white>Dances: " + db);
        }
        List<String> emotes = plugin.getEmoteManager().listEmotes();
        if (emotes.isEmpty()) {
            msg(player, "<gray>No emotes uploaded yet. Drop a <white>gif/png <gray>into "
                    + "<white>plugins/LemonCosmetics/emotes/<gray>.");
            return;
        }
        msg(player, PREFIX + "<white>Available emotes <gray>(" + emotes.size() + "):");
        StringBuilder sb = new StringBuilder();
        for (String e : emotes) {
            sb.append("<click:run_command:'/emote ").append(e).append("'>")
              .append("<hover:show_text:'<green>Click to play'><yellow>").append(e).append("</yellow></hover></click>  ");
        }
        player.sendMessage(MM.deserialize("<!italic>" + sb));
        msg(player, "<gray>/emote stop <dark_gray>— stop early");
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
