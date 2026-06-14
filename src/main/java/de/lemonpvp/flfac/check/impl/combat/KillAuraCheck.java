package de.lemonpvp.flfac.check.impl.combat;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Flags impossible aiming behaviour around attacks: rotation snaps that are too
 * large to be human and physically impossible pitch values.
 */
public final class KillAuraCheck extends Check {

    public KillAuraCheck(FLFAC plugin) {
        super(plugin, CheckType.KILLAURA);
    }

    public void handleAttack(PlayerData data, Player attacker, Entity target) {
        if (!isEnabled()) {
            return;
        }

        // Impossible pitch (client cannot look beyond straight up/down).
        if (Math.abs(data.getPitch()) > 90.5F) {
            flag(data, String.format("pitch=%.1f", data.getPitch()));
            return;
        }

        // Aim snap: a huge instant yaw change on the tick of the attack.
        double maxRotation = settings().getDouble("max-rotation-delta", 50.0D);
        if (data.getDeltaYaw() > maxRotation) {
            flag(data, String.format("yawSnap=%.1f max=%.1f", data.getDeltaYaw(), maxRotation));
        }
    }
}
