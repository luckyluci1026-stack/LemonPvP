package com.lemonpvp.lemonquests.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonquests.LemonQuests;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class QuestsMessaging implements PluginMessageListener {

    private static final String CHANNEL = "lemonquests:main";

    private final LemonQuests plugin;

    public QuestsMessaging(LemonQuests plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregister() {
        Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        try {
            String action = in.readUTF();
            if (!"StatUpdate".equals(action)) return;

            String uuidStr = in.readUTF();
            String statType = in.readUTF();
            int amount = in.readInt();

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[QuestsMessaging] Received StatUpdate with invalid UUID: " + uuidStr);
                return;
            }

            // Dispatch to the quest manager on the main server thread.
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getQuestManager().onStatUpdate(uuid, statType, amount));

        } catch (Exception e) {
            plugin.getLogger().warning("[QuestsMessaging] Failed to parse plugin message: " + e.getMessage());
        }
    }
}
