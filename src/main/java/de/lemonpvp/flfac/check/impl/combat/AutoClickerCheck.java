package de.lemonpvp.flfac.check.impl.combat;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.MathUtil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Detects auto clickers by impossible CPS and by inhuman click consistency
 * (the time between clicks barely varies for a real player).
 */
public final class AutoClickerCheck extends Check {

    public AutoClickerCheck(FLFAC plugin) {
        super(plugin, CheckType.AUTOCLICKER);
    }

    public void recordClick(PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        Deque<Long> clicks = data.getClickTimes();
        clicks.addLast(now);
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }

        int cps = clicks.size();
        int maxCps = settings().getInt("max-cps", 20);
        if (cps > maxCps) {
            flag(data, "cps=" + cps + " max=" + maxCps);
            return;
        }

        // Consistency: very low deviation between intervals is robotic.
        if (cps >= 8) {
            List<Long> times = new ArrayList<>(clicks);
            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < times.size(); i++) {
                intervals.add(times.get(i) - times.get(i - 1));
            }
            double deviation = MathUtil.standardDeviation(intervals);
            double minDeviation = settings().getDouble("constant-cps-deviation", 1.2D);
            if (deviation < minDeviation) {
                flag(data, String.format("cps=%d deviation=%.2fms", cps, deviation));
            }
        }
    }
}
