package de.lemonpvp.flfac.command;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.config.CheckSettings;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** {@code /flfac} command: alerts, verbose, info, checks, reset, reload. */
public final class FLFACCommand implements CommandExecutor, TabCompleter {

    private final FLFAC plugin;

    public FLFACCommand(FLFAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("flfac.command")) {
            send(sender, ColorUtil.mm("<red>You don't have permission to use FLFAC."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "alerts" -> toggleAlerts(sender);
            case "verbose" -> toggleVerbose(sender);
            case "info", "vl" -> info(sender, args);
            case "reset" -> reset(sender, args);
            case "checks" -> checks(sender);
            case "reload" -> reload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void toggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, prefix().append(text("Only players can toggle alerts.", NamedTextColor.RED)));
            return;
        }
        if (!player.hasPermission("flfac.alerts")) {
            send(sender, noPerm());
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        data.setAlertsEnabled(!data.isAlertsEnabled());
        send(sender, prefix().append(accent("Alerts " + (data.isAlertsEnabled() ? "enabled" : "disabled") + ".")));
    }

    private void toggleVerbose(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, prefix().append(text("Only players can toggle verbose.", NamedTextColor.RED)));
            return;
        }
        if (!player.hasPermission("flfac.verbose")) {
            send(sender, noPerm());
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        data.setVerboseEnabled(!data.isVerboseEnabled());
        send(sender, prefix().append(accent("Verbose " + (data.isVerboseEnabled() ? "enabled" : "disabled") + ".")));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, prefix().append(text("Usage: /flfac info <player>", NamedTextColor.RED)));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, prefix().append(text("Player not found.", NamedTextColor.RED)));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().get(target);
        if (data == null) {
            send(sender, prefix().append(text("No data for that player yet.", NamedTextColor.RED)));
            return;
        }

        send(sender, prefix().append(ColorUtil.gradientRaw("Violations of " + target.getName(),
                plugin.getConfigManager().getPlayerGradient())));
        boolean any = false;
        for (CheckType type : CheckType.values()) {
            double vl = data.getViolations(type);
            if (vl <= 0.0D) {
                continue;
            }
            any = true;
            send(sender, Component.text(" • ", NamedTextColor.DARK_GRAY)
                    .append(ColorUtil.gradientRaw(type.getDisplayName(), plugin.getConfigManager().getCheckGradient()))
                    .append(Component.text(" x" + (int) Math.floor(vl), NamedTextColor.GRAY)));
        }
        if (!any) {
            send(sender, Component.text(" Clean - no violations.", NamedTextColor.GREEN));
        }
        send(sender, Component.text(" Ping: ", NamedTextColor.GRAY)
                .append(accent(target.getPing() + "ms"))
                .append(Component.text("  TPS: ", NamedTextColor.GRAY))
                .append(accent(String.format("%.1f", plugin.getCurrentTps()))));
    }

    private void reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("flfac.admin")) {
            send(sender, noPerm());
            return;
        }
        if (args.length < 2) {
            send(sender, prefix().append(text("Usage: /flfac reset <player>", NamedTextColor.RED)));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, prefix().append(text("Player not found.", NamedTextColor.RED)));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().get(target);
        if (data != null) {
            data.resetViolations();
        }
        plugin.getPunishmentManager().clear(target.getUniqueId());
        send(sender, prefix().append(accent("Reset violations of " + target.getName() + ".")));
    }

    private void checks(CommandSender sender) {
        send(sender, prefix().append(accent("Checks (" + plugin.getCheckManager().enabledCount()
                + "/" + plugin.getCheckManager().totalCount() + " active)")));
        for (CheckType type : CheckType.values()) {
            CheckSettings settings = plugin.getConfigManager().getCheck(type);
            Component status = settings.isEnabled()
                    ? Component.text("ON", NamedTextColor.GREEN)
                    : Component.text("OFF", NamedTextColor.RED);
            send(sender, Component.text(" • ", NamedTextColor.DARK_GRAY)
                    .append(ColorUtil.gradientRaw(type.getDisplayName(), plugin.getConfigManager().getCheckGradient()))
                    .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                    .append(status)
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(type.getCategory().name().toLowerCase(Locale.ROOT), NamedTextColor.GRAY)));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("flfac.admin")) {
            send(sender, noPerm());
            return;
        }
        plugin.reload();
        send(sender, prefix().append(accent("Configuration reloaded.")));
    }

    private void sendHelp(CommandSender sender) {
        send(sender, prefix().append(ColorUtil.gradientRaw("Fast Lag Free Anti Cheat",
                plugin.getConfigManager().getPrefixGradient())));
        helpLine(sender, "/flfac alerts", "toggle cheat alerts");
        helpLine(sender, "/flfac verbose", "toggle verbose debug output");
        helpLine(sender, "/flfac info <player>", "show a player's violations");
        helpLine(sender, "/flfac checks", "list all checks and their state");
        if (sender.hasPermission("flfac.admin")) {
            helpLine(sender, "/flfac reset <player>", "reset a player's violations");
            helpLine(sender, "/flfac reload", "reload the configuration");
        }
    }

    private void helpLine(CommandSender sender, String usage, String description) {
        send(sender, ColorUtil.gradientRaw(usage, plugin.getConfigManager().getAccentGradient())
                .append(Component.text(" - " + description, NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("flfac.command")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("alerts", "verbose", "info", "checks"));
            if (sender.hasPermission("flfac.admin")) {
                subs.add("reset");
                subs.add("reload");
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("info") || sub.equals("vl") || sub.equals("reset")) {
                List<String> names = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    names.add(player.getName());
                }
                return filter(names, args[1]);
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }

    // ----- helpers -----
    private Component prefix() {
        return plugin.getAlertManager().prefix();
    }

    private Component accent(String text) {
        return ColorUtil.gradientRaw(ColorUtil.escape(text), plugin.getConfigManager().getAccentGradient());
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    private Component noPerm() {
        return prefix().append(text("You don't have permission for that.", NamedTextColor.RED));
    }

    private void send(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }
}
