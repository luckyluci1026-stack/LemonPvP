package de.lemonpvp.bettersmp.storage;

import java.util.UUID;

/**
 * Ein Ban oder Mute. expires = 0 bedeutet permanent.
 * screen ist nur bei Bans belegt (der Ban-Screen), bei Mutes null.
 */
public record Punishment(
        UUID uuid, String name, String reason, String display, String screen,
        long expires, String actor, long created
) {

    public boolean permanent() {
        return expires <= 0;
    }

    public boolean isExpired() {
        return !permanent() && System.currentTimeMillis() >= expires;
    }

    public long remainingMillis() {
        return permanent() ? Long.MAX_VALUE : Math.max(0, expires - System.currentTimeMillis());
    }
}
