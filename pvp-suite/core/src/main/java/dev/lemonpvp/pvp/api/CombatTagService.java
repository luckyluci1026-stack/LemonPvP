package dev.lemonpvp.pvp.api;

import java.util.UUID;

/**
 * Combat tag state, published by pvp-combattag. The interface lives in pvp-core so that
 * other modules (e.g. pvp-scoreboard) can use it without depending on pvp-combattag: they
 * look it up with {@code services().find(CombatTagService.class)} each time they need it and
 * simply show nothing while pvp-combattag is not running. Main thread only.
 */
public interface CombatTagService {

    /**
     * @param player player UUID
     * @return milliseconds until the player may log out safely, 0 if not in combat
     */
    long remainingMillis(UUID player);

    /**
     * @param player player UUID
     * @return whether the player is in combat
     */
    default boolean isTagged(UUID player) {
        return remainingMillis(player) > 0;
    }
}
