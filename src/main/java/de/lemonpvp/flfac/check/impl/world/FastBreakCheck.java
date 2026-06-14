package de.lemonpvp.flfac.check.impl.world;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * FastBreak detection. We time how long a player spent digging a block and flag
 * near-instant breaks of very hard blocks, which no legitimate tool can do.
 */
public final class FastBreakCheck extends Check {

    public FastBreakCheck(FLFAC plugin) {
        super(plugin, CheckType.FASTBREAK);
    }

    public void handleStartDigging(PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        data.setBreakStartMillis(System.currentTimeMillis());
    }

    public void handleBlockBreak(PlayerData data, Player player, Block block) {
        if (!isEnabled() || player.getGameMode() == GameMode.CREATIVE) {
            data.setBreakStartMillis(0L);
            return;
        }

        float hardness = block.getType().getHardness();
        // Only judge genuinely hard blocks - soft blocks are broken quickly even legitimately.
        if (hardness >= 3.0F) {
            long start = data.getBreakStartMillis();
            long elapsed = start == 0L ? 0L : System.currentTimeMillis() - start;
            double minRatio = settings().getDouble("min-break-ratio", 0.6D);
            // Expected lower bound scales with hardness; anything well under it is impossible.
            long minExpected = (long) (hardness * 60.0D * minRatio);
            if (elapsed < Math.max(150L, minExpected)) {
                flag(data, String.format("%s broken in %dms (hardness %.1f)", block.getType(), elapsed, hardness));
            }
        }
        data.setBreakStartMillis(0L);
    }
}
