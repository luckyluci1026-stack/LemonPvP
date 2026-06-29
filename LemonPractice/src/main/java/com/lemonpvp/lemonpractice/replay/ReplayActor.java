package com.lemonpvp.lemonpractice.replay;

import org.bukkit.Location;

/**
 * A single on-screen actor in a replay (one of the two duelists). Implemented
 * either by a packet-based player NPC ({@link NpcReplayActor}) or a fallback
 * armor stand ({@link ArmorStandReplayActor}).
 */
public interface ReplayActor {

    /** Move/rotate the actor to the given location. */
    void teleport(Location loc);

    /** Despawn and clean up the actor. */
    void remove();
}
