package com.lemonpvp.lemonlobby.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonlobby.LemonLobby;
import org.bukkit.entity.Player;

public class LobbyMessaging {

    private final LemonLobby plugin;

    public LobbyMessaging(LemonLobby plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a BungeeCord "Connect" message to move the player to the given server.
     */
    public void connectToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Sends the player's UUID and chosen training mode over the "lemonlobby:training" channel,
     * then connects the player to the training server.
     */
    public void sendTrainingMode(Player player, String mode) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(mode);
        player.sendPluginMessage(plugin, "lemonlobby:training", out.toByteArray());

        String trainingServer = plugin.getServersConfig().getString("servers.duels.name", "duels");
        connectToServer(player, trainingServer);
    }
}
