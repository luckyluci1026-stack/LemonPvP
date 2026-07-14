package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.gui.KitAdminGUI;
import com.lemonpvp.lemonpractice.model.Gamemode;
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
 * {@code /kitadmin} — create, edit and delete the per-gamemode preset kits.
 * <ul>
 *   <li>{@code /kitadmin} — open the preset manager GUI</li>
 *   <li>{@code /kitadmin edit <gamemode>} — edit a preset directly</li>
 *   <li>{@code /kitadmin save} — save the preset you're editing</li>
 *   <li>{@code /kitadmin cancel} — discard the edit</li>
 *   <li>{@code /kitadmin delete <gamemode>} — revert a preset to kits.yml</li>
 * </ul>
 */
public class KitAdminCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public KitAdminCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("lemonpractice.admin.kits")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission."));
            return true;
        }

        if (args.length == 0) {
            new KitAdminGUI(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (!plugin.getKitAdminManager().save(player)) {
                    msg(player, "<gray>You aren't editing a kit. Use <white>/kitadmin<gray>.");
                }
            }
            case "cancel", "exit" -> {
                if (!plugin.getKitAdminManager().cancel(player)) {
                    msg(player, "<gray>You aren't editing a kit.");
                }
            }
            case "edit" -> {
                if (args.length < 2) { msg(player, "<gray>Usage: /kitadmin edit <gamemode>"); return true; }
                Gamemode gm = findGamemode(args[1]);
                if (gm == null) { msg(player, "<red>Unknown gamemode <yellow>" + args[1] + "<red>."); return true; }
                plugin.getKitAdminManager().enter(player, gm.getId());
            }
            case "delete", "remove" -> {
                if (args.length < 2) { msg(player, "<gray>Usage: /kitadmin delete <gamemode>"); return true; }
                Gamemode gm = findGamemode(args[1]);
                if (gm == null) { msg(player, "<red>Unknown gamemode <yellow>" + args[1] + "<red>."); return true; }
                if (!plugin.getKitManager().hasAdminKit(gm.getId())) {
                    msg(player, "<gray>" + gm.getId() + " already uses the kits.yml default.");
                    return true;
                }
                plugin.getKitManager().deleteAdminKit(gm.getId());
                msg(player, "<yellow>" + gm.getId() + " <green>reverted to the kits.yml default.");
            }
            default -> new KitAdminGUI(plugin, player).open();
        }
        return true;
    }

    private Gamemode findGamemode(String id) {
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            if (gm.getId().equalsIgnoreCase(id)) return gm;
        }
        return null;
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("lemonpractice.admin.kits")) return out;
        if (args.length == 1) {
            for (String s : List.of("edit", "save", "cancel", "delete")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete"))) {
            String p = args[1].toLowerCase(Locale.ROOT);
            for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
                if (gm.getId().startsWith(p)) out.add(gm.getId());
            }
        }
        return out;
    }
}
