package de.lemonpvp.flfac.check.impl.combat;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Flags hits where the attacker is barely (or not) looking at the target -
 * a sign of aimbot/hitbox expansion that lets players hit off-angle.
 */
public final class HitboxCheck extends Check {

    public HitboxCheck(FLFAC plugin) {
        super(plugin, CheckType.HITBOX);
    }

    public void handleAttack(PlayerData data, Player attacker, Entity target) {
        if (!isEnabled()) {
            return;
        }

        Vector look = attacker.getEyeLocation().getDirection();
        Vector toTarget = target.getBoundingBox().getCenter().subtract(attacker.getEyeLocation().toVector());
        if (toTarget.lengthSquared() < 1.0E-6) {
            return;
        }
        double angle = Math.toDegrees(look.angle(toTarget.normalize()));
        double max = settings().getDouble("max-angle", 75.0D);

        if (angle > max) {
            flag(data, String.format("angle=%.1f max=%.1f", angle, max));
        }
    }
}
