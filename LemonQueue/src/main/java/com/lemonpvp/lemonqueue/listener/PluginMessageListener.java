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

        // Internal backend→proxy control message: consume it so it is not
        // forwarded onward to the client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            if ("PlayerBanning".equals(action)) {
                UUID uuid = UUID.fromString(in.readUTF());
                queueManager.markBanning(uuid);
                // Extended payload (newer LemonCore): expiry epoch ms + MiniMessage
                // kick screen — cached so future logins are denied at the proxy.
                // Older senders stop after the uuid; the EOF here is expected.
                try {
                    long expiryEpochMs = Long.parseLong(in.readUTF());
                    String kickScreen = in.readUTF();
                    queueManager.cacheBan(uuid, expiryEpochMs, kickScreen);
                } catch (IOException | NumberFormatException ignored) {}
            } else if ("PlayerKicking".equals(action)) {
                String uuidStr = in.readUTF();
                queueManager.markKicking(UUID.fromString(uuidStr));
            } else if ("PlayerUnbanning".equals(action)) {
                // Backend lifted a ban — clear the login-deny cache immediately.
                queueManager.uncacheBan(UUID.fromString(in.readUTF()));
            }
        } catch (IOException | IllegalArgumentException ignored) {}
    }
}
