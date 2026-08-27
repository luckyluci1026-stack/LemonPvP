package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.tournament.Tournament;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /tournament} on the duels server — where tournaments actually live,
 * since they are ranked 1v1s. Player view (join, list, standings, finals) plus
 * admin management (create, open, close, champion).
 */
public class TournamentCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String ADMIN = "lemonpractice.admin.tournament";

    private final LemonPractice plugin;

    public TournamentCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Bare /tournament (or /tournament gui) opens the click-driven panel.
        if (sender instanceof Player p && (args.length == 0 || args[0].equalsIgnoreCase("gui"))) {
            new com.lemonpvp.lemonpractice.gui.TournamentGUI(plugin, p).open();
            return true;
        }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> join(sender, args);
            case "list" -> list(sender);
            case "top", "standings" -> standings(sender, args);
            case "finals" -> finals(sender, args);
            case "create" -> { if (admin(sender)) create(sender, args); }
            case "open" -> { if (admin(sender)) open(sender, args); }
            case "close" -> { if (admin(sender)) close(sender, args); }
            case "champion" -> { if (admin(sender)) champion(sender, args); }
            default -> help(sender);
        }
        return true;
    }

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return; }
        Tournament t = args.length >= 2 ? byId(args[1]) : plugin.getTournamentManager().activeJoinable();
        if (t == null) { msg(p, "<red>No tournament to join right now."); return; }
        plugin.getTournamentManager().signup(p.getUniqueId(), t.getId()).thenAccept(ok ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (Boolean.TRUE.equals(ok)) msg(p, "<green>You signed up for <white>" + t.getName()
                            + "<green>! Play <white>" + t.getGamemode() + " <green>ranked to climb.");
                    else msg(p, "<gray>You're already signed up (or sign-ups are closed).");
                }));
    }

    private void list(CommandSender sender) {
        var all = plugin.getTournamentManager().all();
        if (all.isEmpty()) { msg(sender, "<gray>No tournaments right now."); return; }
        msg(sender, "<gradient:#fffb00:#00ff00><bold>Tournaments</bold></gradient>");
        for (Tournament t : all) {
            msg(sender, "<dark_gray>#<white>" + t.getId() + " <gray>" + t.getName()
                    + " <dark_gray>| <gray>" + t.getGamemode() + " <dark_gray>| <yellow>" + t.getState());
        }
    }

    private void standings(CommandSender sender, String[] args) {
        Tournament t = args.length >= 2 ? byId(args[1]) : plugin.getTournamentManager().activeJoinable();
        if (t == null) { msg(sender, "<red>No such tournament."); return; }
        plugin.getTournamentManager().standings(t.getId()).thenAccept(rows -> Bukkit.getScheduler().runTask(plugin, () -> {
            msg(sender, "<gradient:#fffb00:#00ff00><bold>" + t.getName() + "</bold></gradient> <dark_gray>» standings");
            if (rows == null || rows.isEmpty()) { msg(sender, "<gray>No wins recorded yet."); return; }
            int rank = 1;
            for (var s : rows) {
                if (rank > 15) break;
                msg(sender, "<gray>#" + (rank++) + " <white>" + nameOf(s.uuid()) + " <dark_gray>(" + s.wins() + " wins)");
            }
        }));
    }

    private void finals(CommandSender sender, String[] args) {
        Tournament t = args.length >= 2 ? byId(args[1]) : latest();
        if (t == null) { msg(sender, "<red>No such tournament."); return; }
        plugin.getTournamentManager().finalists(t.getId()).thenAccept(rows -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (rows == null || rows.isEmpty()) { msg(sender, "<gray>Finalists aren't set yet for <white>" + t.getName() + "<gray>."); return; }
            msg(sender, "<gradient:#fffb00:#00ff00><bold>" + t.getName() + "</bold></gradient> <dark_gray>» finalists");
            int seed = 1;
            for (var s : rows) msg(sender, "<gray>#" + (seed++) + " <white>" + nameOf(s.uuid()) + " <dark_gray>(" + s.wins() + " wins)");
        }));
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 3) { msg(sender, "<red>Usage: <white>/tournament create <gamemode> <name>"); return; }
        String gamemode = args[1];
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        plugin.getTournamentManager().create(name, gamemode).thenAccept(t -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (t != null) msg(sender, "<green>Created tournament <white>#" + t.getId() + " " + t.getName()
                    + " <green>(" + t.getGamemode() + "). Open it with <white>/tournament open " + t.getId());
            else msg(sender, "<red>Failed to create the tournament.");
        }));
    }

    private void open(CommandSender sender, String[] args) {
        Tournament t = args.length >= 2 ? byId(args[1]) : null;
        if (t == null) { msg(sender, "<red>Usage: <white>/tournament open <id> [days]"); return; }
        int days = plugin.getTournamentManager().defaultDays();
        if (args.length >= 3) { try { days = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {} }
        if (plugin.getTournamentManager().openQualification(t.getId(), days))
            msg(sender, "<green>Qualification open for <white>" + days + " days<green>.");
        else msg(sender, "<red>Could not open (already started?).");
    }

    private void close(CommandSender sender, String[] args) {
        Tournament t = args.length >= 2 ? byId(args[1]) : null;
        if (t == null) { msg(sender, "<red>Usage: <white>/tournament close <id>"); return; }
        plugin.getTournamentManager().closeToFinals(t.getId(), plugin.getTournamentManager().finalsSize());
        msg(sender, "<green>Closing qualification and locking finalists for <white>" + t.getName() + "<green>.");
    }

    private void champion(CommandSender sender, String[] args) {
        if (args.length < 3) { msg(sender, "<red>Usage: <white>/tournament champion <id> <player>"); return; }
        Tournament t = byId(args[1]);
        if (t == null) { msg(sender, "<red>No such tournament."); return; }
        String name = args[2];
        resolveUuid(name).thenAccept(uuid -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (uuid == null) { msg(sender, "<red>Player not found: <white>" + name); return; }
            plugin.getTournamentManager().setChampion(t.getId(), uuid);
            msg(sender, "<green>Crowned <white>" + name + " <green>as champion of <white>" + t.getName() + "<green>.");
        }));
    }

    private void help(CommandSender s) {
        msg(s, "<gradient:#fffb00:#00ff00><bold>Tournaments</bold></gradient>");
        msg(s, "<yellow>/tournament join [id] <gray>— sign up");
        msg(s, "<yellow>/tournament list <gray>— all tournaments");
        msg(s, "<yellow>/tournament top [id] <gray>— standings");
        msg(s, "<yellow>/tournament finals [id] <gray>— finalists");
        if (s.hasPermission(ADMIN)) {
            msg(s, "<gold>/tournament create <gamemode> <name>");
            msg(s, "<gold>/tournament open <id> [days] <dark_gray>| close <id> | champion <id> <player>");
        }
    }

    // -- helpers -------------------------------------------------------------

    private boolean admin(CommandSender s) {
        if (s.hasPermission(ADMIN)) return true;
        msg(s, "<red>You don't have permission.");
        return false;
    }

    private Tournament byId(String s) {
        try { return plugin.getTournamentManager().get(Integer.parseInt(s)); }
        catch (NumberFormatException e) { return null; }
    }

    private Tournament latest() {
        Tournament best = null;
        for (Tournament t : plugin.getTournamentManager().all())
            if (best == null || t.getId() > best.getId()) best = t;
        return best;
    }

    /** Resolves a player name to a UUID: online first, then LemonCore's store. */
    private CompletableFuture<UUID> resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return CompletableFuture.completedFuture(online.getUniqueId());
        org.bukkit.plugin.Plugin lc = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lc instanceof com.lemonpvp.lemoncore.LemonCore core) {
            return core.getPlayerDataManager().findUUIDByName(name);
        }
        return CompletableFuture.completedFuture(null);
    }

    private String nameOf(UUID id) {
        var p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

    private void msg(CommandSender s, String mini) { s.sendMessage(MM.deserialize("<!italic>" + mini)); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "list", "top", "finals"));
            if (sender.hasPermission(ADMIN)) subs.addAll(List.of("create", "open", "close", "champion"));
            List<String> out = new ArrayList<>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        if (args.length == 2 && "create".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN)) {
            // Suggest real gamemode ids from the duels server's own registry.
            List<String> out = new ArrayList<>();
            for (var gm : plugin.getGamemodeManager().getAllGamemodes()) {
                if (gm.getId().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(gm.getId());
            }
            return out;
        }
        if (args.length == 2 && sender.hasPermission(ADMIN)
                && List.of("open", "close", "champion").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> out = new ArrayList<>();
            for (Tournament t : plugin.getTournamentManager().all())
                if (String.valueOf(t.getId()).startsWith(args[1])) out.add(String.valueOf(t.getId()));
            return out;
        }
        if (args.length == 3 && "champion".equalsIgnoreCase(args[0]) && sender.hasPermission(ADMIN)) {
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(p.getName());
            return out;
        }
        return List.of();
    }
}
