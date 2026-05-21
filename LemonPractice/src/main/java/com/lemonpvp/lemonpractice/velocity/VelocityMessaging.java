package com.lemonpvp.lemonpractice.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class VelocityMessaging implements PluginMessageListener {

    public static final String BUNGEECORD = "BungeeCord";
    public static final String LEMON_CHANNEL = "lemonpractice:main";

    private final LemonPractice plugin;

    public VelocityMessaging(LemonPractice plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEECORD);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, LEMON_CHANNEL, this);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, LEMON_CHANNEL);
    }

    public void unregister() {
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEECORD);
        Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, LEMON_CHANNEL);
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, LEMON_CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(LEMON_CHANNEL)) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        try {
            String action = in.readUTF();
            switch (action) {
                case "QueueMatch" -> {
                    // Two players matched — sent to duel server together
                    String p1 = in.readUTF();
                    String p2 = in.readUTF();
                    String gamemode = in.readUTF();
                    plugin.getQueueManager().handleMatchFound(UUID.fromString(p1), UUID.fromString(p2), gamemode);
                }
                case "JoinQueue" -> {
                    // Player wants to queue for a gamemode (from lobby)
                    String uuid = in.readUTF();
                    String gamemode = in.readUTF();
                    plugin.getQueueManager().addToQueue(UUID.fromString(uuid), gamemode);
                }
                case "LeaveQueue" -> {
                    String uuid = in.readUTF();
                    plugin.getQueueManager().removeFromQueue(UUID.fromString(uuid));
                }
                case "PlayerCounts" -> {
                    // Response: gamemode, playing, queuing
                    String gamemode = in.readUTF();
                    int playing = in.readInt();
                    int queuing = in.readInt();
                    plugin.getQueueManager().updateCounts(gamemode, playing, queuing);
                }
                default -> {}
            }
        } catch (Exception ignored) {}
    }

    public void sendToServer(Player via, String serverName) {
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        via.sendPluginMessage(plugin, BUNGEECORD, out.toByteArray());
    }

    public void sendLemonMessage(Player via, String action, String... args) {
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(action);
        for (String arg : args) out.writeUTF(arg);
        via.sendPluginMessage(plugin, LEMON_CHANNEL, out.toByteArray());
    }

    public void requestPlayerCounts(Player via) {
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RequestCounts");
        via.sendPluginMessage(plugin, LEMON_CHANNEL, out.toByteArray());
    }

    public Player getAnyOnlinePlayer() {
        return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    }
}
