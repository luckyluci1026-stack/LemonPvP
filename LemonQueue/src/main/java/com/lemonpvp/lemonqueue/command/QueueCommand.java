package com.lemonpvp.lemonqueue.command;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.lemonpvp.lemonqueue.queue.QueueManager;
import com.lemonpvp.lemonqueue.queue.ServerQueue;
import com.lemonpvp.lemonqueue.util.Gradients;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * {@code /lemonqueue} (aliases {@code /lq}, {@code /queue}).
 *
 * <ul>
 *   <li>no args – show your position, or overall stats for admins</li>
 *   <li>{@code leave} – leave the queue (stay parked in limbo)</li>
 *   <li>{@code admin} – per-server queue sizes (needs admin permission)</li>
 *   <li>{@code clear <server>} – empty a server's queue (needs admin permission)</li>
 * </ul>
 */
public class QueueCommand implements SimpleCommand {

    private static final String ADMIN_PERM = "lemonqueue.admin";

    private final LemonQueue plugin;
    private final QueueManager queues;
    private final QueueConfig config;

    public QueueCommand(LemonQueue plugin, QueueManager queues, QueueConfig config) {
        this.plugin = plugin;
        this.queues = queues;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            if (source instanceof Player player) {
                int pos = queues.positionOf(player.getUniqueId());
                if (pos > 0) {
                    player.sendMessage(Gradients.lemonAnimated("Du stehst in der Warteschlange – Platz " + pos + "."));
                } else {
                    player.sendMessage(Gradients.lemon("Du stehst aktuell in keiner Warteschlange."));
                }
            } else {
                showAdminStats(source);
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "leave" -> {
                if (source instanceof Player player) {
                    if (queues.positionOf(player.getUniqueId()) > 0) {
                        queues.dequeue(player.getUniqueId());
                        player.clearTitle();
                        player.sendMessage(Gradients.fire("Du hast die Warteschlange verlassen."));
                    } else {
                        player.sendMessage(Component.text("Du stehst in keiner Warteschlange.", NamedTextColor.GRAY));
                    }
                } else {
                    source.sendMessage(Component.text("Nur Spieler können die Warteschlange verlassen.", NamedTextColor.RED));
                }
            }
            case "admin" -> {
                if (!source.hasPermission(ADMIN_PERM)) { noPerm(source); return; }
                showAdminStats(source);
            }
            case "clear" -> {
                if (!source.hasPermission(ADMIN_PERM)) { noPerm(source); return; }
                if (args.length < 2) {
                    source.sendMessage(Component.text("Nutzung: /lq clear <server>", NamedTextColor.RED));
                    return;
                }
                queues.clearQueue(args[1]);
                source.sendMessage(Gradients.lemon("Warteschlange für '" + args[1] + "' geleert."));
            }
            default -> source.sendMessage(Component.text(
                    "Unbekannt. Nutzung: /lq [leave|admin|clear <server>]", NamedTextColor.RED));
        }
    }

    private void showAdminStats(com.velocitypowered.api.command.CommandSource source) {
        source.sendMessage(Gradients.rainbowAnimated("══════ LemonQueue ══════"));
        if (queues.getQueues().isEmpty()) {
            source.sendMessage(Component.text("Keine aktiven Warteschlangen.", NamedTextColor.GRAY));
            return;
        }
        for (ServerQueue q : queues.getQueues().values()) {
            int max = config.getMaxPlayers(q.getTargetServer());
            String cap = max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);
            source.sendMessage(Component.text()
                    .append(Gradients.lemon(q.getTargetServer()))
                    .append(Component.text(" – " + q.size() + " wartend (max " + cap + ")", NamedTextColor.GRAY))
                    .build());
        }
        source.sendMessage(Component.text("Gesamt: " + queues.totalQueued() + " Spieler", NamedTextColor.DARK_GRAY));
    }

    private void noPerm(com.velocitypowered.api.command.CommandSource source) {
        source.sendMessage(Component.text("Dazu fehlt dir die Berechtigung.", NamedTextColor.RED));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // base command open to all; subcommands gate themselves
    }
}
