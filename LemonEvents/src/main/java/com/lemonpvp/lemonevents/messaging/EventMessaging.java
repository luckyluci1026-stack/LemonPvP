package com.lemonpvp.lemonevents.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonevents.LemonEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class EventMessaging {

    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final LemonEvents plugin;

    public EventMessaging(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    public void unregister() {
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    /** Transfers a player to the given server. */
    public void sendToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, BUNGEE_CHANNEL, out.toByteArray());
    }

    /**
     * Broadcasts a raw legacy-formatted message to ALL players on ALL servers.
     * Uses a random online player as the routing vehicle.
     */
    public void broadcastToAll(String legacyMessage) {
        Player via = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF("ALL");
        out.writeUTF(legacyMessage);
        via.sendPluginMessage(plugin, BUNGEE_CHANNEL, out.toByteArray());
    }
}
