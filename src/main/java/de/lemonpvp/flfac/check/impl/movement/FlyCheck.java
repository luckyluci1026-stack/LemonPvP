package de.lemonpvp.flfac.check.impl.movement;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MovementUtil;
import org.bukkit.entity.Player;

/**
 * Detects hovering and gravity-defying flight: staying airborne for many ticks
 * without ever falling (dy stays at/above ~0).
 */
public final class FlyCheck extends Check {

    public FlyCheck(FLFAC plugin) {
        super(plugin, CheckType.FLY);
    }

    public void handleMove(PlayerData data, Player player, double dy, boolean onGround) {
        if (!isEnabled() || MovementUtil.isExempt(player)) {
            data.setAirTicks(0);
            return;
        }
        long now = System.currentTimeMillis();
        if (data.inVelocityGrace(now)) {
            data.setAirTicks(0);
            return;
        }

        if (onGround) {
            data.setAirTicks(0);
            return;
        }

        data.setAirTicks(data.getAirTicks() + 1);
        int maxHover = settings().getInt("max-air-hover-ticks", 12);

        // Real players always start falling (dy clearly negative) due to gravity.
        // Hovering or rising for too long in the air is not possible legitimately.
        if (data.getAirTicks() > maxHover && dy > -0.05D) {
            flag(data, String.format("airTicks=%d dy=%.3f", data.getAirTicks(), dy));
        }
    }
}
