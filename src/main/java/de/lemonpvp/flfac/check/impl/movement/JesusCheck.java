package de.lemonpvp.flfac.check.impl.movement;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MovementUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Jesus / water-walk detection: moving horizontally on top of liquid without
 * sinking and without being submerged, a boat, or a lily pad.
 */
public final class JesusCheck extends Check {

    public JesusCheck(FLFAC plugin) {
        super(plugin, CheckType.JESUS);
    }

    public void handleMove(PlayerData data, Player player, double dx, double dy, double dz, boolean onGround) {
        if (!isEnabled()) {
            return;
        }
        if (player.isInsideVehicle() || player.isFlying() || player.getAllowFlight() || player.isGliding()) {
            return;
        }
        // Submerged players are swimming normally, not walking on top.
        if (player.isInWater() && player.getEyeLocation().getBlock().getType() == Material.WATER) {
            return;
        }

        Location loc = player.getLocation();
        Material below = MovementUtil.blockBelow(loc).getType();
        Material feet = loc.getBlock().getType();

        boolean overLiquid = MovementUtil.isLiquid(below) && !MovementUtil.isLiquid(feet);
        boolean onLilyOrSolid = feet == Material.LILY_PAD;
        boolean horizontalMove = Math.hypot(dx, dz) > 0.05D;
        boolean notSinking = dy > -0.03D;

        if (overLiquid && horizontalMove && notSinking && !onLilyOrSolid) {
            flag(data, String.format("over=%s dy=%.3f", below, dy));
        }
    }
}
