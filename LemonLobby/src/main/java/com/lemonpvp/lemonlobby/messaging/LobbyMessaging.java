package com.lemonpvp.lemonlobby.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonlobby.LemonLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

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
        String trainingServer = plugin.getServersConfig().getString("servers.practice.name", "practice");
        UUID uuid = player.getUniqueId();
        // Save pending mode before transferring so the training server can read it on join
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().savePendingTrainingMode(uuid, mode);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) connectToServer(p, trainingServer);
            });
        });
    }
}
