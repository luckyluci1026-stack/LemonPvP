package de.lemonpvp.flfac.check.impl.world;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Scaffold detection: flags placing blocks the player is barely (or not) facing,
 * which is typical for scaffold/bridge modules that place behind the player.
 */
public final class ScaffoldCheck extends Check {

    public ScaffoldCheck(FLFAC plugin) {
        super(plugin, CheckType.SCAFFOLD);
    }

    public void handleBlockPlace(PlayerData data, Player player, Block block) {
        if (!isEnabled()) {
            return;
        }

        Vector look = player.getEyeLocation().getDirection();
        Vector toBlock = block.getLocation().toVector()
                .add(new Vector(0.5D, 0.5D, 0.5D))
                .subtract(player.getEyeLocation().toVector());
        if (toBlock.lengthSquared() < 1.0E-6) {
            return;
        }
        double angle = Math.toDegrees(look.angle(toBlock.normalize()));
        double max = settings().getDouble("max-place-angle", 80.0D);

        if (angle > max) {
            flag(data, String.format("placeAngle=%.1f max=%.1f", angle, max));
        }
    }
}
