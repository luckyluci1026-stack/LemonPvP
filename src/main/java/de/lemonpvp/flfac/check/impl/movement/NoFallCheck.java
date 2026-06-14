package de.lemonpvp.flfac.check.impl.movement;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MovementUtil;
import org.bukkit.entity.Player;

/**
 * Detects NoFall: the client claims to be on ground while falling, so the
 * server never builds up any fall distance even though the player is dropping.
 */
public final class NoFallCheck extends Check {

    public NoFallCheck(FLFAC plugin) {
        super(plugin, CheckType.NOFALL);
    }

    public void handleMove(PlayerData data, Player player, double dy, boolean onGround) {
        if (!isEnabled() || MovementUtil.isExempt(player)) {
            data.setAirFallDistance(0.0D);
            return;
        }

        if (onGround || dy >= 0.0D) {
            data.setAirFallDistance(0.0D);
            return;
        }

        // Falling: accumulate how far we have dropped.
        data.setAirFallDistance(data.getAirFallDistance() + (-dy));
        double minFall = settings().getDouble("min-fall-distance", 3.0D);

        // A legit fall makes the server accumulate getFallDistance(). NoFall
        // keeps it near zero because the client lies about being grounded.
        if (data.getAirFallDistance() >= minFall && player.getFallDistance() < 1.0F) {
            flag(data, String.format("fell=%.1f serverFall=%.1f", data.getAirFallDistance(), player.getFallDistance()));
            data.setAirFallDistance(0.0D);
        }
    }
}
