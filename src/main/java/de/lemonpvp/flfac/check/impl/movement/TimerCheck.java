package de.lemonpvp.flfac.check.impl.movement;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;

/**
 * Timer / game-speed check. Movement packets should arrive roughly every 50ms.
 * A running "balance" of how far ahead the client is flags clients that send
 * packets too quickly (speed/timer hacks).
 */
public final class TimerCheck extends Check {

    private static final long EXPECTED_INTERVAL_MS = 50L;

    public TimerCheck(FLFAC plugin) {
        super(plugin, CheckType.TIMER);
    }

    public void handleMove(PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = data.getLastMoveMillis();
        data.setLastMoveMillis(now);

        if (last == 0L) {
            return;
        }

        long elapsed = now - last;
        // Big gaps (teleports, lag spikes) reset the balance instead of flagging.
        if (elapsed > 1000L) {
            data.setTimerBalance(0.0D);
            return;
        }

        double balance = data.getTimerBalance() + (EXPECTED_INTERVAL_MS - elapsed);
        double maxBalance = settings().getDouble("max-balance", 120.0D);

        if (balance > maxBalance) {
            flag(data, String.format("balance=%.0fms", balance));
            balance = 0.0D;
        } else if (balance < -1000.0D) {
            balance = -1000.0D; // clamp so a slow period cannot mask a later speedup
        }
        data.setTimerBalance(balance);
    }
}
