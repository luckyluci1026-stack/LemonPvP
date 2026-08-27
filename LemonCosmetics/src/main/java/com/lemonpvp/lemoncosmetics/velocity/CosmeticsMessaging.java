package com.lemonpvp.lemoncosmetics.velocity;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArmorSlotType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class CosmeticsMessaging implements PluginMessageListener {

    private static final String CHANNEL = "lemoncosmetics:main";

    // Outbound action identifiers
    private static final String ACTION_EFFECT_UNLOCKED = "EffectUnlocked";
    private static final String ACTION_ACTIVE_EFFECT    = "ActiveEffect";
    private static final String ACTION_TRIM_APPLIED     = "TrimApplied";

    private final LemonCosmetics plugin;

    public CosmeticsMessaging(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }

    // -------------------------------------------------------------------------
    // Outbound helpers
    // -------------------------------------------------------------------------

    public void sendEffectUnlocked(Player player, String effectId) {
        send(player, out -> {
            out.writeUTF(ACTION_EFFECT_UNLOCKED);
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(effectId);
        });
    }

    public void sendActiveEffect(Player player, String effectId) {
        send(player, out -> {
            out.writeUTF(ACTION_ACTIVE_EFFECT);
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(effectId == null ? "" : effectId);
        });
    }

    public void sendTrimApplied(Player player, ArmorSlotType slot, String patternId, String materialId) {
        send(player, out -> {
            out.writeUTF(ACTION_TRIM_APPLIED);
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(slot.name().toLowerCase());
            out.writeUTF(patternId);
            out.writeUTF(materialId);
        });
    }

    // -------------------------------------------------------------------------
    // Inbound
    // -------------------------------------------------------------------------

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String action = in.readUTF();
            UUID uuid = UUID.fromString(in.readUTF());

            switch (action) {
                case ACTION_EFFECT_UNLOCKED -> {
                    String effectId = in.readUTF();
                    plugin.getCosmeticsManager().unlockKillEffect(uuid, effectId);
                }
                case ACTION_ACTIVE_EFFECT -> {
                    String effectId = in.readUTF();
                    plugin.getCosmeticsManager().setActiveKillEffect(uuid, effectId.isEmpty() ? null : effectId);
                }
                case ACTION_TRIM_APPLIED -> {
                    String slotName = in.readUTF();
                    String patternId = in.readUTF();
                    String materialId = in.readUTF();
                    try {
                        ArmorSlotType slot = ArmorSlotType.valueOf(slotName.toUpperCase());
                        plugin.getCosmeticsManager().applyArmorTrim(uuid, slot, patternId, materialId);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown armor slot in plugin message: " + slotName);
                    }
                }
                default -> plugin.getLogger().warning("Unknown cosmetics message action: " + action);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to parse cosmetics plugin message: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface DataWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private void send(Player player, DataWriter writer) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            writer.write(out);
            player.sendPluginMessage(plugin, CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send cosmetics plugin message: " + e.getMessage());
        }
    }
}
