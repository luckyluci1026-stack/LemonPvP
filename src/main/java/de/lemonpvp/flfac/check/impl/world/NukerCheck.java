package de.lemonpvp.flfac.check.impl.world;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Nuker detection: breaking more than a handful of blocks within a single
 * server tick is not humanly possible in survival.
 */
public final class NukerCheck extends Check {

    public NukerCheck(FLFAC plugin) {
        super(plugin, CheckType.NUKER);
    }

    public void handleBlockBreak(PlayerData data, Player player) {
        if (!isEnabled() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        long tick = plugin.getTick();
        if (data.getBlockTickStamp() == tick) {
            data.setBlocksThisTick(data.getBlocksThisTick() + 1);
        } else {
            data.setBlockTickStamp(tick);
            data.setBlocksThisTick(1);
        }

        int max = settings().getInt("max-blocks-per-tick", 2);
        if (data.getBlocksThisTick() > max) {
            flag(data, "blocks/tick=" + data.getBlocksThisTick() + " max=" + max);
        }
    }
}
