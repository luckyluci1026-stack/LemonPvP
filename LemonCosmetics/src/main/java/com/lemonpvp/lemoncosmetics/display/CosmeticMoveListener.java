package com.lemonpvp.lemoncosmetics.display;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import org.bukkit.entity.Player;

/**
 * Task 4 — the "no-lag tracker". PacketEvents fires {@link #onPacketReceive} on
 * the Netty I/O thread the instant a client movement packet arrives, i.e. before
 * the server has even ticked. We read the new position straight from the packet
 * and immediately push a teleport for the player's cosmetics, so a cape/hat stays
 * pixel-perfectly glued to the body instead of trailing a tick behind (which is
 * what happens if you only move cosmetics from a server-tick task).
 *
 * <p>All the work is delegated to {@link DisplayCosmeticManager#onMovement} /
 * {@code onRotation}, which are written to be safe off the main thread.</p>
 */
public class CosmeticMoveListener extends PacketListenerAbstract {

    private final DisplayCosmeticManager manager;

    public CosmeticMoveListener(DisplayCosmeticManager manager) {
        super(PacketListenerPriority.MONITOR); // observe only; never modify movement
        this.manager = manager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        java.util.UUID uuid = player.getUniqueId();
        if (!manager.hasCosmetics(uuid)) return;

        var type = event.getPacketType();
        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            var wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            var pos = wrapper.getPosition();
            manager.onMovement(uuid, pos.getX(), pos.getY(), pos.getZ(), wrapper.getYaw());
        } else if (type == PacketType.Play.Client.PLAYER_POSITION) {
            var wrapper = new WrapperPlayClientPlayerPosition(event);
            var pos = wrapper.getPosition();
            manager.onMovement(uuid, pos.getX(), pos.getY(), pos.getZ(), null);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
            var wrapper = new WrapperPlayClientPlayerRotation(event);
            manager.onRotation(uuid, wrapper.getYaw());
        }
    }
}
