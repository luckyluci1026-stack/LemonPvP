package com.lemonpvp.lemonpractice.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Makes bot hit-registration reliable: the bot is an invisible zombie (real
 * hitbox) with a fake packet player-model drawn on top. A player attacking the
 * visible model sends an INTERACT_ENTITY packet targeting the FAKE entity id,
 * which the server can't resolve — so the hit silently drops. This listener
 * catches those attacks and forwards them to the zombie hitbox on the main
 * thread. Only registered when PacketEvents is present.
 */
public class BotHitPacketListener extends PacketListenerAbstract {

    private final LemonPractice plugin;

    public BotHitPacketListener(LemonPractice plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        if (!(event.getPlayer() instanceof Player attacker)) return;
        final int entityId = wrapper.getEntityId();

        // Hop to the main thread: Bukkit entity/damage APIs aren't thread-safe.
        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getBotDuelManager().handleNpcHit(attacker, entityId));
    }
}
