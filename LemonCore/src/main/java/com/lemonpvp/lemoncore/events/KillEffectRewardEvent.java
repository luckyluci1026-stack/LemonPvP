package com.lemonpvp.lemoncore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class KillEffectRewardEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String effectId;

    public KillEffectRewardEvent(UUID playerUuid, String effectId) {
        this.playerUuid = playerUuid;
        this.effectId = effectId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getEffectId() {
        return effectId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
