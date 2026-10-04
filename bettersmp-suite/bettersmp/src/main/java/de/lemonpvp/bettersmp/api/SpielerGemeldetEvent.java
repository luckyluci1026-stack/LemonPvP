package de.lemonpvp.bettersmp.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class SpielerGemeldetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID melder;
    private final String melderName;
    private final UUID gemeldet;
    private final String gemeldetName;
    private final String grund;

    public SpielerGemeldetEvent(UUID melder, String melderName, UUID gemeldet, String gemeldetName, String grund) {
        this.melder = melder;
        this.melderName = melderName;
        this.gemeldet = gemeldet;
        this.gemeldetName = gemeldetName;
        this.grund = grund;
    }

    public UUID melder() {
        return melder;
    }

    public String melderName() {
        return melderName;
    }

    public UUID gemeldet() {
        return gemeldet;
    }

    public String gemeldetName() {
        return gemeldetName;
    }

    public String grund() {
        return grund;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
