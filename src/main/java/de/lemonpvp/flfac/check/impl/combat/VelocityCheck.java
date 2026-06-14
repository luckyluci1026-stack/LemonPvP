package de.lemonpvp.flfac.check.impl.combat;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;

/**
 * Anti-knockback detection. When the server applies knockback we remember the
 * expected velocity and compare it to how the player actually moved on the
 * following tick. Far too little movement means the knockback was cancelled.
 */
public final class VelocityCheck extends Check {

    public VelocityCheck(FLFAC plugin) {
        super(plugin, CheckType.VELOCITY);
    }

    public void onVelocity(PlayerData data, double x, double y, double z) {
        if (!isEnabled()) {
            return;
        }
        data.setPendingVelocity(x, y, z);
    }

    public void handleMove(PlayerData data, double dx, double dy, double dz) {
        if (!isEnabled() || !data.hasPendingVelocity()) {
            return;
        }

        data.tickVelocity();
        if (data.getVelocityTicks() < 1) {
            return;
        }

        double expectedH = Math.hypot(data.getPendingVelX(), data.getPendingVelZ());
        double actualH = Math.hypot(dx, dz);
        double minH = settings().getDouble("min-horizontal", 0.20D);
        double minV = settings().getDouble("min-vertical", 0.10D);

        if (expectedH > 0.10D && actualH < expectedH * minH) {
            flag(data, String.format("h-took=%.0f%% expected=%.2f", (actualH / expectedH) * 100.0D, expectedH));
        } else if (data.getPendingVelY() > 0.10D && dy < data.getPendingVelY() * minV) {
            flag(data, String.format("v-took=%.0f%% expected=%.2f", (dy / data.getPendingVelY()) * 100.0D, data.getPendingVelY()));
        }

        data.clearVelocity();
    }
}
