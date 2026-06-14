package de.lemonpvp.flfac.alert;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.config.ConfigManager;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Builds and dispatches FLFAC's gradient alerts and verbose output. */
public final class AlertManager {

    private final FLFAC plugin;

    public AlertManager(FLFAC plugin) {
        this.plugin = plugin;
    }

    /** The FLFAC brand prefix used in front of every message. */
    public Component prefix() {
        ConfigManager c = plugin.getConfigManager();
        return ColorUtil.mm(
                ColorUtil.gradientWrap("<bold>" + ColorUtil.escape(c.getPrefixText()) + "</bold>", c.getPrefixGradient())
                        + " <dark_gray>»</dark_gray> ");
    }

    public void alert(Check check, PlayerData data, double vl, String details) {
        Component message = buildLine(check, data, vl, details, false);

        ConfigManager c = plugin.getConfigManager();
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("flfac.alerts")) {
                continue;
            }
            PlayerData staffData = plugin.getPlayerDataManager().get(staff);
            if (staffData == null || !staffData.isAlertsEnabled()) {
                continue;
            }
            staff.sendMessage(message);
            playAlertSound(staff);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public void verbose(Check check, PlayerData data, double vl, String details) {
        Component message = buildLine(check, data, vl, details, true);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("flfac.verbose")) {
                continue;
            }
            PlayerData staffData = plugin.getPlayerDataManager().get(staff);
            if (staffData != null && staffData.isVerboseEnabled()) {
                staff.sendMessage(message);
            }
        }
    }

    private Component buildLine(Check check, PlayerData data, double vl, String details, boolean verbose) {
        ConfigManager c = plugin.getConfigManager();
        Player player = data.getPlayer();
        int ping = player != null ? player.getPing() : 0;
        double tps = plugin.getCurrentTps();

        String checkName = check.getType().getDisplayName();
        String safeDetails = ColorUtil.escape(details == null ? "" : details);

        String line = c.getAlertFormat()
                .replace("%player%", ColorUtil.gradientWrap(ColorUtil.escape(data.getName()), c.getPlayerGradient()))
                .replace("%check%", ColorUtil.gradientWrap(checkName, c.getCheckGradient()))
                .replace("%type%", "<gray>" + check.getType().getCategory().name() + "</gray>")
                .replace("%vl%", ColorUtil.gradientWrap(String.valueOf((int) Math.floor(vl)), c.getAccentGradient()))
                .replace("%ping%", String.valueOf(ping))
                .replace("%tps%", String.format("%.1f", tps))
                .replace("%details%", "<gray>" + safeDetails + "</gray>");

        Component body = ColorUtil.mm(line);
        if (verbose) {
            body = ColorUtil.mm("<dark_gray>[V]</dark_gray> ").append(body);
        }

        Component result = prefix().append(body);

        // Hover with the full breakdown.
        Component hover = Component.text()
                .append(ColorUtil.gradientRaw("FLFAC", c.getPrefixGradient()))
                .append(Component.text(" Detection", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(hoverLine("Player", data.getName()))
                .append(hoverLine("Check", checkName + " (" + check.getType().getCategory().name() + ")"))
                .append(hoverLine("Violations", String.valueOf((int) Math.floor(vl))))
                .append(hoverLine("Ping", ping + "ms"))
                .append(hoverLine("TPS", String.format("%.1f", tps)))
                .append(hoverLine("Info", safeDetails.isEmpty() ? "-" : details))
                .build();

        return result
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.suggestCommand("/flfac info " + data.getName()));
    }

    private Component hoverLine(String key, String value) {
        return Component.text()
                .append(Component.text(key + ": ", NamedTextColor.GRAY))
                .append(ColorUtil.gradientRaw(ColorUtil.escape(value), plugin.getConfigManager().getAccentGradient()))
                .append(Component.newline())
                .build();
    }

    private void playAlertSound(Player staff) {
        ConfigManager c = plugin.getConfigManager();
        if (!c.isAlertSoundEnabled()) {
            return;
        }
        try {
            staff.playSound(staff.getLocation(), c.getAlertSoundName(), c.getAlertSoundVolume(), c.getAlertSoundPitch());
        } catch (Exception ignored) {
            // Invalid sound key in config - silently ignore so alerts still show.
        }
    }
}
