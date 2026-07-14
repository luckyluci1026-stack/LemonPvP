package com.lemonpvp.lemoncosmetics.commands;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.cape.MapCosmeticSlot;
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
 * Drives {@code /cape} and {@code /bandana} — the same map-cosmetic command for
 * both {@link MapCosmeticSlot}s, differing only in slot/label/permission:
 * <ul>
 *   <li>{@code /<cmd>} or {@code /<cmd> list} — list uploaded files</li>
 *   <li>{@code /<cmd> <name>} — wear it</li>
 *   <li>{@code /<cmd> off} — remove it</li>
 *   <li>{@code /<cmd> reload} — re-apply with current config placement (tuning)</li>
 * </ul>
 * Files live in {@code plugins/LemonCosmetics/<slot-folder>/} (png/gif/jpg).
 */
public class MapCosmeticCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCosmetics plugin;
    private final MapCosmeticSlot slot;
    private final String label;
    private final String permission;
    private final String prefix;

    public MapCosmeticCommand(LemonCosmetics plugin, MapCosmeticSlot slot, String label, String permission) {
        this.plugin = plugin;
        this.slot = slot;
        this.label = label;
        this.permission = permission;
        this.prefix = "<gradient:#ffe259:#ffa751><bold>" + capitalize(label)
                + "</bold></gradient> <dark_gray>»</dark_gray> ";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String lbl, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission(permission)) { msg(player, "<red>You don't have permission."); return true; }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listFiles(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "off", "remove", "none" -> {
                plugin.getCapeManager().unequip(player.getUniqueId(), slot);
                msg(player, "<gray>" + capitalize(label) + " removed.");
            }
            case "reload" -> {
                String current = plugin.getCapeManager().current(player.getUniqueId(), slot);
                if (current == null) { msg(player, "<gray>You aren't wearing a " + label + "."); return true; }
                plugin.reloadConfig();
                if (plugin.getCapeManager().equip(player, slot, current)) {
                    msg(player, "<green>Re-applied <yellow>" + current + " <green>with current placement.");
                } else {
                    msg(player, "<red>Could not re-apply <yellow>" + current + "<red>.");
                }
            }
            default -> {
                String name = args[0];
                if (plugin.getCapeManager().equip(player, slot, name)) {
                    msg(player, "<green>Now wearing <yellow>" + name + "<green>.");
                } else {
                    msg(player, "<red>" + capitalize(label) + " <yellow>" + name
                            + " <red>not found or unreadable. Try <white>/" + label + " list<red>.");
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission(permission)) return List.of();
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String sub : List.of("list", "off", "reload")) if (sub.startsWith(prefix)) out.add(sub);
        for (String file : plugin.getCapeManager().list(slot)) {
            if (file.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(file);
        }
        return out;
    }

    private void listFiles(Player player) {
        List<String> files = plugin.getCapeManager().list(slot);
        if (files.isEmpty()) {
            msg(player, "<gray>No " + label + "s uploaded yet. Drop an image into "
                    + "<white>plugins/LemonCosmetics/" + slot.folder + "/<gray>.");
            return;
        }
        msg(player, prefix + "<white>Available " + label + "s <gray>(" + files.size() + "):");
        StringBuilder sb = new StringBuilder();
        for (String c : files) {
            sb.append("<click:run_command:'/").append(label).append(' ').append(c).append("'>")
              .append("<hover:show_text:'<green>Click to wear'><yellow>").append(c).append("</yellow></hover></click>  ");
        }
        player.sendMessage(MM.deserialize("<!italic>" + sb));
        msg(player, "<gray>/" + label + " off <dark_gray>— remove");
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
