package com.lemonpvp.lemonqueue.command;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.Messages;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.lemonpvp.lemonqueue.queue.QueueManager;
import com.lemonpvp.lemonqueue.queue.ServerQueue;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

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

    private static final String ADMIN_PERM   = "lemonqueue.admin";
    private static final String RELOAD_PERM  = "lemonqueue.admin";

    private final LemonQueue plugin;
    private final QueueManager queues;

    public QueueCommand(LemonQueue plugin, QueueManager queues) {
        this.plugin = plugin;
        this.queues = queues;
    }

    // Read live so /lq reload (which swaps the config + Messages bundle) applies.
    private QueueConfig config() { return plugin.getConfig(); }
    private Messages msg()       { return plugin.getConfig().getMessages(); }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();
        Messages msg = msg();

        if (args.length == 0) {
            if (source instanceof Player player) {
                int pos = queues.positionOf(player.getUniqueId());
                if (pos > 0) {
                    player.sendMessage(msg.cmdPosition(pos));
                } else {
                    player.sendMessage(msg.cmdNotQueued());
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
                        player.disconnect(msg.leaveProxy());
                    } else {
                        player.sendMessage(msg.cmdNotInQueue());
                    }
                } else {
                    source.sendMessage(msg.cmdPlayersOnly());
                }
            }
            case "admin" -> {
                if (!source.hasPermission(ADMIN_PERM)) { noPerm(source); return; }
                showAdminStats(source);
            }
            case "clear" -> {
                if (!source.hasPermission(ADMIN_PERM)) { noPerm(source); return; }
                if (args.length < 2) {
                    source.sendMessage(msg.cmdClearUsage());
                    return;
                }
                queues.clearQueue(args[1]);
                source.sendMessage(msg.cmdCleared(args[1]));
            }
            case "reload" -> {
                if (!source.hasPermission(RELOAD_PERM)) { noPerm(source); return; }
                plugin.reload();
                source.sendMessage(msg.cmdReloaded());
            }
            case "clearbans" -> {
                // Escape hatch: instantly drop every cached login-deny so an
                // unbanned player can rejoin without waiting for the re-check.
                if (!source.hasPermission(ADMIN_PERM)) { noPerm(source); return; }
                int n = queues.clearBanCache();
                source.sendMessage(net.kyori.adventure.text.Component.text(
                        "Cleared " + n + " cached ban(s) from the proxy login filter."));
            }
            default -> source.sendMessage(msg.cmdUnknown());
        }
    }

    private void showAdminStats(com.velocitypowered.api.command.CommandSource source) {
        Messages msg = msg();
        QueueConfig config = config();
        source.sendMessage(msg.adminHeader());
        if (queues.getQueues().isEmpty()) {
            source.sendMessage(msg.adminNone());
            return;
        }
        for (ServerQueue q : queues.getQueues().values()) {
            int max = config.getMaxPlayers(q.getTargetServer());
            String cap = max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);
            source.sendMessage(msg.adminLine(q.getTargetServer(), q.size(), cap));
        }
        source.sendMessage(msg.adminTotal(queues.totalQueued()));
    }

    private void noPerm(com.velocitypowered.api.command.CommandSource source) {
        source.sendMessage(msg().cmdNoPerm());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // base command open to all; subcommands gate themselves
    }
}
