package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** {@code /helden3 ...} - alle Adminwerkzeuge des Projekts. */
public final class AdminCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "status", "herzen", "link", "raus", "zurueck", "dummy", "item", "reset", "spawn");

    /** Schuetzt {@code /helden3 reset} vor dem versehentlichen Ausfuehren. */
    private static final String RESET_CONFIRMATION = "bestaetigen";

    public AdminCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.admin", false);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            plugin.messages().sendList(sender, "admin.usage");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "status" -> status(sender);
            case "herzen", "hearts" -> hearts(sender, args);
            case "link" -> link(sender, args);
            case "raus", "out" -> eliminate(sender, args);
            case "zurueck", "restore" -> restore(sender, args);
            case "dummy" -> dummy(sender, args);
            case "item" -> giveItem(sender, args);
            case "reset" -> reset(sender, args);
            case "spawn" -> setSpawn(sender);
            default -> plugin.messages().send(sender, "general.unknown-subcommand", "%usage%", "/helden3");
        }
    }

    private void reload(CommandSender sender) {
        long started = System.currentTimeMillis();
        plugin.reloadAll();
        plugin.messages().send(sender, "general.reloaded", "%ms%", System.currentTimeMillis() - started);
    }

    private void status(CommandSender sender) {
        plugin.messages().sendList(sender, "admin.status",
                "%start%", plugin.settings().startHearts(),
                "%link%", plugin.settings().linkHeartEnabled() ? "ja" : "nein",
                "%pvponly%", plugin.settings().pvpOnly() ? "ja" : "nein",
                "%participants%", plugin.game().participants(),
                "%alive%", plugin.game().alive().size(),
                "%dummies%", plugin.dummies().size(),
                "%items%", plugin.items().size(),
                "%profiles%", plugin.profiles().size(),
                "%floodgate%", plugin.bedrock().isFloodgateHooked() ? "ja" : "nein");
    }

    private void hearts(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().sendText(sender, "&7/helden3 herzen <spieler> <set|add|remove> <anzahl>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        Integer amount = parseInt(sender, args[3]);
        if (amount == null) {
            return;
        }

        int target = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "add" -> profile.hearts() + amount;
            case "remove" -> profile.hearts() - amount;
            default -> amount;
        };
        plugin.hearts().setHearts(profile, target);
        plugin.messages().send(sender, "hearts.set",
                "%player%", profile.name(),
                "%hearts%", profile.hearts());
    }

    private void link(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().sendText(sender, "&7/helden3 link <spieler> <partner|clear>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }

        if (args[2].equalsIgnoreCase("clear")) {
            plugin.links().clear(profile);
            plugin.messages().send(sender, "link.cleared", "%player%", profile.name());
            return;
        }

        HeldenProfile partner = plugin.profiles().findByName(args[2]);
        if (partner == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[2]);
            return;
        }
        profile.linkPartner(partner.uuid());
        plugin.messages().send(sender, "admin.link-set",
                "%player%", profile.name(),
                "%partner%", partner.name());
        plugin.hud().updateAll();
    }

    private void eliminate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendText(sender, "&7/helden3 raus <spieler>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        if (profile.eliminated()) {
            plugin.messages().send(sender, "game.already-eliminated", "%player%", profile.name());
            return;
        }
        plugin.game().eliminate(profile, null, new HashSet<>());
    }

    private void restore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendText(sender, "&7/helden3 zurueck <spieler>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        plugin.game().restore(profile);
    }

    private void dummy(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("clear")) {
            plugin.messages().sendText(sender, "&7/helden3 dummy clear");
            return;
        }
        int removed = plugin.dummies().removeAll() + plugin.dummies().removeOrphans();
        plugin.messages().send(sender, "admin.dummies-cleared", "%amount%", removed);
    }

    private void giveItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendText(sender, "&7/helden3 item <id> [spieler] [anzahl]");
            return;
        }
        if (!plugin.items().exists(args[1])) {
            plugin.messages().send(sender, "item.unknown",
                    "%item%", args[1],
                    "%available%", String.join(", ", plugin.items().ids()));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                plugin.messages().send(sender, "general.unknown-player", "%player%", args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.messages().send(sender, "general.player-only");
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = parseInt(sender, args[3]);
            if (parsed == null) {
                return;
            }
            amount = Math.max(1, parsed);
        }

        ItemStack stack = plugin.items().stack(args[1], amount);
        if (stack == null) {
            return;
        }
        target.getInventory().addItem(stack);

        String display = plugin.items().get(args[1]).display();
        plugin.messages().send(target, "item.given", "%amount%", amount, "%item%", display);
        if (!target.equals(sender)) {
            plugin.messages().send(sender, "item.given-other",
                    "%player%", target.getName(),
                    "%amount%", amount,
                    "%item%", display);
        }
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase(RESET_CONFIRMATION)) {
            plugin.messages().send(sender, "admin.reset-confirm");
            return;
        }
        plugin.hearts().resetAll();
        plugin.messages().send(sender, "game.reset",
                "%players%", plugin.profiles().size(),
                "%hearts%", plugin.settings().totalStartHearts());
    }

    private void setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return;
        }
        plugin.settings().writeSpawn(player.getLocation());
        plugin.saveConfig();
        plugin.messages().send(sender, "admin.spawn-set",
                "%world%", player.getWorld().getName(),
                "%x%", (int) player.getLocation().getX(),
                "%y%", (int) player.getLocation().getY(),
                "%z%", (int) player.getLocation().getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "item" -> filter(plugin.items().ids(), args[1]);
                case "dummy" -> filter(List.of("clear"), args[1]);
                case "reset" -> filter(List.of(RESET_CONFIRMATION), args[1]);
                case "herzen", "hearts", "link", "raus", "out", "zurueck", "restore" -> onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (sub) {
                case "herzen", "hearts" -> filter(List.of("set", "add", "remove"), args[2]);
                case "link" -> {
                    List<String> options = new java.util.ArrayList<>(onlinePlayerNames(""));
                    options.add("clear");
                    yield filter(options, args[2]);
                }
                case "item" -> onlinePlayerNames(args[2]);
                default -> List.of();
            };
        }
        return List.of();
    }
}
