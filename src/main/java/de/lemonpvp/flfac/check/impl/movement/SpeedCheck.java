package de.lemonpvp.flfac.check.impl.movement;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MovementUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Horizontal speed check. Compares the per-tick horizontal distance against a
 * generous, modifier-aware limit (speed potions, ice, jump bursts).
 */
public final class SpeedCheck extends Check {

    public SpeedCheck(FLFAC plugin) {
        super(plugin, CheckType.SPEED);
    }

    public void handleMove(PlayerData data, Player player, double dx, double dz, boolean onGround) {
        if (!isEnabled() || MovementUtil.isExempt(player)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (data.inVelocityGrace(now)) {
            return;
        }

        double horizontal = Math.hypot(dx, dz);
        double allowed = settings().getDouble("max-horizontal", 0.62D);

        PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            allowed *= 1.0D + 0.2D * (speed.getAmplifier() + 1);
        }
        // Ice and slime allow noticeably faster movement.
        if (MovementUtil.isOnIce(player.getLocation()) || MovementUtil.isOnSlime(player.getLocation())) {
            allowed *= 1.4D;
        }
        // Brief sprint-jump bursts: allow extra right after leaving the ground.
        if (!onGround && data.isLastOnGround()) {
            allowed *= 1.6D;
        }

        if (horizontal > allowed) {
            flag(data, String.format("h=%.3f allowed=%.3f", horizontal, allowed));
        }
    }
}
