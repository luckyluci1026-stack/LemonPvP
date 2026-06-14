package de.lemonpvp.flfac.check.impl.combat;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MathUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Flags hits that land further away than survival reach allows. Uses the
 * closest point of the target's hitbox and (optionally) lag-compensates the
 * allowed distance based on the attacker's ping.
 */
public final class ReachCheck extends Check {

    public ReachCheck(FLFAC plugin) {
        super(plugin, CheckType.REACH);
    }

    public void handleAttack(PlayerData data, Player attacker, Entity target) {
        if (!isEnabled() || attacker.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Vector eye = attacker.getEyeLocation().toVector();
        BoundingBox box = target.getBoundingBox();

        double cx = MathUtil.clamp(eye.getX(), box.getMinX(), box.getMaxX());
        double cy = MathUtil.clamp(eye.getY(), box.getMinY(), box.getMaxY());
        double cz = MathUtil.clamp(eye.getZ(), box.getMinZ(), box.getMaxZ());
        double distance = eye.distance(new Vector(cx, cy, cz));

        double allowed = settings().getDouble("max-reach", 3.05D);
        if (plugin.getConfigManager().isLagCompensation()) {
            // Up to +0.5 blocks of tolerance for laggy players.
            allowed += Math.min(0.5D, attacker.getPing() / 600.0D);
        }

        if (distance > allowed) {
            flag(data, String.format("dist=%.2f allowed=%.2f", distance, allowed));
        }
    }
}
