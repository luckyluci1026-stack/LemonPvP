package de.lemonpvp.flfac.check.impl.player;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.Location;

/**
 * Sanity check for impossible movement packets: out-of-range pitch and
 * non-finite coordinates that a vanilla client can never send.
 */
public final class BadPacketsCheck extends Check {

    public BadPacketsCheck(FLFAC plugin) {
        super(plugin, CheckType.BADPACKETS);
    }

    public void handleMove(PlayerData data, Location to) {
        if (!isEnabled()) {
            return;
        }

        float pitch = to.getPitch();
        if (Math.abs(pitch) > 90.5F) {
            flag(data, String.format("pitch=%.1f", pitch));
            return;
        }

        if (!Float.isFinite(pitch) || !Float.isFinite(to.getYaw())
                || !Double.isFinite(to.getX()) || !Double.isFinite(to.getY()) || !Double.isFinite(to.getZ())) {
            flag(data, "non-finite movement values");
        }
    }
}
