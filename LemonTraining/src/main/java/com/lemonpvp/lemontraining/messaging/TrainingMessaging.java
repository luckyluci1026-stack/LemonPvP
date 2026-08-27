package com.lemonpvp.lemontraining.messaging;

import com.google.common.io.ByteStreams;
import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class TrainingMessaging implements PluginMessageListener {

    private final LemonTraining plugin;

    public TrainingMessaging(LemonTraining plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("lemonlobby:training")) return;

        com.google.common.io.ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String uuidStr;
        String modeName;
        try {
            uuidStr  = in.readUTF();
            modeName = in.readUTF();
        } catch (Exception e) {
            plugin.getLogger().warning("Malformed lemonlobby:training message");
            return;
        }

        UUID uuid = UUID.fromString(uuidStr);
        PracticeMode mode;
        try {
            mode = PracticeMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown practice mode: " + modeName);
            return;
        }

        // Schedule on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(uuid);
            if (target == null) {
                // Player not yet arrived — store as pending
                plugin.getPracticeManager().schedulePending(uuid, mode);
            } else {
                plugin.getPracticeManager().startPractice(target, mode);
            }
        });
    }
}
