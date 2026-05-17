package com.lemonpvp.lemoncore.velocity;

import com.lemonpvp.lemoncore.LemonCore;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class VelocityMessaging implements PluginMessageListener {

    public static final String CHANNEL = "BungeeCord";
    public static final String LEMON_CHANNEL = "lemonpvp:core";

    private final LemonCore plugin;

    public VelocityMessaging(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, LEMON_CHANNEL, this);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, LEMON_CHANNEL);
    }

    public void unregister() {
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
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
                case "BanPlayer" -> {
                    String uuidStr = in.readUTF();
                    String reason = in.readUTF();
                    // Handle ban notification from proxy
                    Player target = Bukkit.getPlayer(UUID.fromString(uuidStr));
                    if (target != null) {
                        plugin.getBanManager().getActiveBan(UUID.fromString(uuidStr))
                                .thenAccept(ban -> {
                                    if (ban != null) {
                                        Bukkit.getScheduler().runTask(plugin, () ->
                                                plugin.getListenerManager().performBanKick(target, ban));
                                    }
                                });
                    }
                }
                case "Restart" -> {
                    // Proxy tells us to restart
                    plugin.getRestartManager().beginRestart();
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
        via.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    public void sendLemonMessage(Player via, String action, String... args) {
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(action);
        for (String arg : args) out.writeUTF(arg);
        via.sendPluginMessage(plugin, LEMON_CHANNEL, out.toByteArray());
    }

    public void broadcastToBungee(Player via, String message) {
        if (via == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF("ALL");
        out.writeUTF(message);
        via.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    public Player getAnyOnlinePlayer() {
        return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    }
}
