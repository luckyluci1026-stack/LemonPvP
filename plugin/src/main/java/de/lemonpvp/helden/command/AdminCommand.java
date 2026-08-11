package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.team.HeldenTeam;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /helden3 ...} - alle Adminwerkzeuge des Projekts. */
public final class AdminCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS =
            List.of("reload", "status", "item", "held", "team", "leben", "coins", "event", "spawn");

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
            case "item" -> giveItem(sender, args);
            case "held" -> setHero(sender, args);
            case "team" -> setTeam(sender, args);
            case "leben" -> setLives(sender, args);
            case "coins" -> setCoins(sender, args);
            case "event" -> handleEvent(sender, args);
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
                "%heroes%", plugin.heroes().size(),
                "%abilities%", plugin.abilities().size(),
                "%items%", plugin.items().size(),
                "%teams%", plugin.teams().size(),
                "%events%", plugin.events().size(),
                "%profiles%", plugin.profiles().size(),
                "%floodgate%", plugin.bedrock().isFloodgateHooked() ? "ja" : "nein");
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

    private void setHero(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().sendText(sender, "&7/helden3 held <spieler> <held>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        Hero hero = plugin.heroes().get(args[2]);
        if (hero == null) {
            plugin.messages().send(sender, "hero.unknown", "%hero%", args[2]);
            return;
        }

        plugin.heroes().apply(target, hero);
        plugin.messages().send(target, "hero.selected", "%hero%", hero.display());
        plugin.messages().send(sender, "hero.current", "%hero%", hero.display());
    }

    private void setTeam(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().sendText(sender, "&7/helden3 team <spieler> <team>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        HeldenTeam team = plugin.teams().get(args[2]);
        if (team == null) {
            plugin.messages().send(sender, "team.unknown", "%team%", args[2]);
            return;
        }

        plugin.teams().assign(profile, team);
        plugin.messages().send(sender, "team.moved", "%player%", profile.name(), "%team%", team.display());

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null) {
            plugin.messages().send(online, "team.joined", "%team%", team.display());
            plugin.hud().update(online);
        }
    }

    private void setLives(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().sendText(sender, "&7/helden3 leben <spieler> <anzahl>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        Integer lives = parseInt(sender, args[2]);
        if (lives == null) {
            return;
        }

        profile.lives(lives);
        profile.fallen(lives <= 0);
        plugin.messages().send(sender, "lives.set", "%player%", profile.name(), "%lives%", profile.lives());

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null) {
            if (profile.fallen()) {
                plugin.lives().applyFallenState(online);
            } else if (online.getGameMode() == GameMode.SPECTATOR) {
                online.setGameMode(GameMode.SURVIVAL);
            }
            plugin.hud().update(online);
        }
    }

    private void setCoins(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().sendText(sender, "&7/helden3 coins <spieler> <anzahl>");
            return;
        }
        HeldenProfile profile = plugin.profiles().findByName(args[1]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[1]);
            return;
        }
        Integer amount = parseInt(sender, args[2]);
        if (amount == null) {
            return;
        }

        plugin.economy().set(profile, amount);
        plugin.messages().send(sender, "economy.set", "%player%", profile.name(), "%amount%", profile.coins());
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendText(sender, "&7/helden3 event <id|stop>");
            return;
        }
        if (args[1].equalsIgnoreCase("stop")) {
            plugin.events().stopEvent();
            return;
        }
        if (plugin.events().get(args[1]) == null) {
            plugin.messages().send(sender, "events.unknown",
                    "%event%", args[1],
                    "%available%", String.join(", ", plugin.events().ids()));
            return;
        }
        if (plugin.events().isRunning()) {
            plugin.messages().send(sender, "events.already-running",
                    "%event%", plugin.events().active().displayName());
            return;
        }
        if (!plugin.events().startEvent(args[1])) {
            plugin.messages().send(sender, "events.start-failed", "%event%", args[1]);
        }
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
                case "event" -> {
                    List<String> options = new ArrayList<>(plugin.events().ids());
                    options.add("stop");
                    yield filter(options, args[1]);
                }
                case "held", "team", "leben", "coins" -> onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (sub) {
                case "held" -> filter(plugin.heroes().ids(), args[2]);
                case "team" -> filter(plugin.teams().ids(), args[2]);
                case "item" -> onlinePlayerNames(args[2]);
                default -> List.of();
            };
        }
        return List.of();
    }
}
