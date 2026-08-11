package de.lemonpvp.smpcontent.command;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /smpcontent give|list|reload
 */
public final class ContentCommand implements TabExecutor {

    /** Übersichts-GUI: zeigt alle Inhalte, Klick gibt das Item. */
    public static final class ContentHolder implements InventoryHolder {
        private Inventory inventory;
        public final List<String> ids = new ArrayList<>();

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final SMPContent plugin;

    public ContentCommand(SMPContent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smpcontent.admin")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msgs().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.msgs().reload();
                plugin.registry().load();

                ConfigProblem.Report problem = plugin.configProblem() != null
                        ? plugin.configProblem()
                        : plugin.msgs().problem();
                if (problem == null) {
                    plugin.msgs().send(sender, "reloaded");
                } else {
                    plugin.msgs().send(sender, "config-error",
                            "file", problem.file(),
                            "line", String.valueOf(problem.line()),
                            "hint", ConfigProblem.safeForChat(problem.hint()));
                    for (String line : problem.context()) {
                        sender.sendMessage(Text.mm("<dark_gray>"
                                + ConfigProblem.safeForChat(line) + "</dark_gray>"));
                    }
                }
            }
            case "list" -> {
                if (sender instanceof Player player) {
                    openList(player);
                } else {
                    plugin.msgs().send(sender, "list-console",
                            "list", String.join(", ", plugin.registry().entries().keySet()));
                }
            }
            case "give" -> give(sender, args);
            case "pack" -> {
                plugin.msgs().send(sender, "pack-building");
                var result = plugin.pack().build();
                if (!result.ok()) {
                    plugin.msgs().send(sender, "pack-failed", "error", result.message());
                } else {
                    plugin.msgs().send(sender, "pack-done",
                            "items", String.valueOf(result.items()),
                            "blocks", String.valueOf(result.blocks()),
                            "models", String.valueOf(result.customModels()));
                    if (result.missing() > 0) {
                        plugin.msgs().send(sender, "pack-missing",
                                "count", String.valueOf(result.missing()),
                                "details", result.message());
                    }
                }
            }
            default -> plugin.msgs().send(sender, "usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msgs().send(sender, "give-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.msgs().send(sender, "player-not-found");
            return;
        }
        CustomEntry entry = plugin.registry().get(args[2]);
        if (entry == null) {
            plugin.msgs().send(sender, "unknown-content", "id", args[2],
                    "list", String.join(", ", plugin.registry().entries().keySet()));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        target.getInventory().addItem(plugin.registry().create(entry, amount));
        plugin.msgs().send(sender, "given", "amount", String.valueOf(amount),
                "id", entry.id(), "player", target.getName());
    }

    /** Baut das Übersichts-GUI mit allen eigenen Inhalten. */
    public void openList(Player player) {
        var all = new ArrayList<>(plugin.registry().entries().values());
        int rows = Math.max(1, Math.min(6, (all.size() + 8) / 9));
        ContentHolder holder = new ContentHolder();
        Inventory inv = Bukkit.createInventory(holder, rows * 9,
                de.lemonpvp.smpcontent.util.Text.mm(plugin.msgs().raw("gui-title")));
        holder.inventory = inv;

        for (int i = 0; i < all.size() && i < rows * 9; i++) {
            CustomEntry entry = all.get(i);
            ItemStack icon = plugin.registry().create(entry, 1);
            ItemMeta meta = icon.getItemMeta();
            List<Component> lore = meta.lore() == null
                    ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(de.lemonpvp.smpcontent.util.Text
                    .mm("<dark_gray>Id: <gray>" + entry.id())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(de.lemonpvp.smpcontent.util.Text
                    .mm("<yellow>Klick <gray>gibt dir dieses " + (entry.block() ? "Block" : "Item"))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(i, icon);
            holder.ids.add(entry.id());
        }
        // freie Plätze auffüllen
        for (int i = all.size(); i < rows * 9; i++) {
            ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = pane.getItemMeta();
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
            inv.setItem(i, pane);
            holder.ids.add(null);
        }
        player.openInventory(inv);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "list", "pack", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (String id : plugin.registry().entries().keySet()) {
                if (id.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
        }
        return out;
    }
}
