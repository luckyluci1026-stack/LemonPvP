package com.lemonpvp.lemonfailover;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /failover} (aliases {@code /lemonfailover}, {@code /lfo}) — inspect and
 * control the failover state.
 * <ul>
 *   <li>{@code status} — show current state + primary reachability</li>
 *   <li>{@code active} — pin ACTIVE (failover) until {@code auto}</li>
 *   <li>{@code standby} — pin STANDBY until {@code auto}</li>
 *   <li>{@code auto} — return control to the automatic monitor</li>
 *   <li>{@code reload} — reload config.yml</li>
 * </ul>
 */
public final class FailoverCommand implements SimpleCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PERMISSION = "lemonfailover.admin";
    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>Failover</bold></gradient> <dark_gray>» ";

    private final LemonFailover plugin;

    public FailoverCommand(LemonFailover plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        if (!src.hasPermission(PERMISSION)) {
            src.sendMessage(MM.deserialize("<red>You don't have permission."));
            return;
        }

        String[] args = invocation.arguments();
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        PrimaryMonitor monitor = plugin.getMonitor();

        switch (sub) {
            case "status" -> sendStatus(src);
            case "active" -> {
                monitor.forceActive();
                src.sendMessage(MM.deserialize(PREFIX + "<red>Pinned to <bold>ACTIVE</bold> (failover). "
                        + "<gray>Use <white>/failover auto <gray>to release."));
            }
            case "standby" -> {
                monitor.forceStandby();
                src.sendMessage(MM.deserialize(PREFIX + "<yellow>Pinned to <bold>STANDBY</bold>. "
                        + "<gray>Use <white>/failover auto <gray>to release."));
            }
            case "auto" -> {
                monitor.resumeAuto();
                src.sendMessage(MM.deserialize(PREFIX + "<green>Automatic monitoring resumed."));
            }
            case "reload" -> {
                plugin.reload();
                src.sendMessage(MM.deserialize(PREFIX + "<green>Configuration reloaded."));
            }
            default -> src.sendMessage(MM.deserialize(
                    "<gray>/failover <white>status <gray>| <white>active <gray>| <white>standby <gray>| <white>auto <gray>| <white>reload"));
        }
    }

    private void sendStatus(CommandSource src) {
        PrimaryMonitor m = plugin.getMonitor();
        FailoverConfig cfg = plugin.getConfig();
        String stateColor = m.getState() == FailoverState.ACTIVE ? "<red>" : "<green>";
        src.sendMessage(MM.deserialize(PREFIX + "<gray>Status"));
        src.sendMessage(MM.deserialize("<gray>State: " + stateColor + "<bold>" + m.getState() + "</bold>"
                + (m.isManualOverride() ? " <dark_gray>(manual)" : " <dark_gray>(auto)")));
        src.sendMessage(MM.deserialize("<gray>Primary <white>" + cfg.getPrimaryHost() + ":" + cfg.getPrimaryPort()
                + " <gray>is " + (m.isPrimaryReachable() ? "<green>reachable" : "<red>unreachable")));
        src.sendMessage(MM.deserialize("<gray>Checks: <white>" + m.getConsecutiveFailures()
                + " <gray>fail / <white>" + m.getConsecutiveSuccesses() + " <gray>ok in a row"));
        src.sendMessage(MM.deserialize("<gray>Deny logins in standby: <white>" + cfg.isDenyLogins()));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) return List.of();
        String[] args = invocation.arguments();
        List<String> subs = List.of("status", "active", "standby", "auto", "reload");
        if (args.length == 0) return subs;
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return subs.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        return List.of();
    }
}
