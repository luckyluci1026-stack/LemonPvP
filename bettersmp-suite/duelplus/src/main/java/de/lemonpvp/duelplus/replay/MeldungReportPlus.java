package de.lemonpvp.duelplus.replay;

import de.lemonpvp.reportplus.api.SpielerGemeldetEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class MeldungReportPlus implements Listener {

    private final ReplayManager replays;

    public MeldungReportPlus(ReplayManager replays) {
        this.replays = replays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beiMeldung(SpielerGemeldetEvent event) {
        replays.spielerGemeldet(event.gemeldet(), event.gemeldetName());
    }
}
