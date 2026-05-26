package com.lemonpvp.lemonlobby.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonlobby.LemonLobby;
import org.bukkit.Bukkit;
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
     * Saves the player's chosen training mode to the shared database so the
     * training server can pick it up on join, then sends the player to the
     * practice/training server.
     */
    public void sendTrainingMode(Player player, String mode) {
        // Write pending mode asynchronously — will be ready before player arrives
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabase().savePendingTrainingMode(player.getUniqueId(), mode));

        String trainingServer = plugin.getServersConfig().getString("servers.practice.name", "practice");
        connectToServer(player, trainingServer);
    }
}
