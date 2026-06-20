package com.lemonpvp.lemonqueue.listener;

import com.lemonpvp.lemonqueue.queue.QueueManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * Listens for messages from LemonCore backend servers on the {@code lemonpvp:core}
 * channel. When LemonCore is about to ban-kick a player, it sends a
 * {@code "PlayerBanning"} message so that LemonQueue knows NOT to intercept
 * that player's {@code KickedFromServerEvent} and re-route them to the limbo.
 */
public class PluginMessageListener {

    private static final String CHANNEL = "lemonpvp:core";

    private final QueueManager queueManager;

    public PluginMessageListener(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            if ("PlayerBanning".equals(action)) {
                String uuidStr = in.readUTF();
                queueManager.markBanning(UUID.fromString(uuidStr));
            }
        } catch (IOException | IllegalArgumentException ignored) {}
    }
}
