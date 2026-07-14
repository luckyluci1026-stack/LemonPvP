package com.lemonpvp.lemonevents.model;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A lightweight, host-driven event (as opposed to the scripted {@code GameEvent}
 * games): a person with the host permission builds a kit, broadcasts, gathers
 * players into a vanilla world and runs a last-one-standing fight. Exactly one
 * is active at a time, owned by {@link com.lemonpvp.lemonevents.managers.HostedEventManager}.
 */
public class HostedEvent {

    public enum State {
        /** Host is building the event kit. */
        SETUP,
        /** Broadcast out, accepting joins. */
        OPEN,
        /** Players are fighting in the arena. */
        RUNNING,
        /** Finished / cleaned up. */
        ENDED
    }

    private final UUID host;
    private final String hostName;
    private final String name;

    // The event kit every participant receives (cloned per player on apply).
    private ItemStack[] kitContents = new ItemStack[36];
    private ItemStack[] kitArmor = new ItemStack[4];
    private ItemStack kitOffhand;

    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();

    private State state = State.SETUP;
    /** True while the pre-fight countdown holds players in place. */
    private boolean frozen = true;

    public HostedEvent(UUID host, String hostName, String name) {
        this.host = host;
        this.hostName = hostName;
        this.name = name;
    }

    public UUID getHost() { return host; }
    public String getHostName() { return hostName; }
    public String getName() { return name; }

    public ItemStack[] getKitContents() { return kitContents; }
    public ItemStack[] getKitArmor() { return kitArmor; }
    public ItemStack getKitOffhand() { return kitOffhand; }
    public void setKit(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        this.kitContents = contents;
        this.kitArmor = armor;
        this.kitOffhand = offhand;
    }

    public Set<UUID> getParticipants() { return participants; }
    public Set<UUID> getAlive() { return alive; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
}
