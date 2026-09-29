package de.lemonpvp.duelplus.replay;

import de.lemonpvp.bettersmp.api.SpielerGemeldetEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class MeldungBetterSmp implements Listener {

    private final ReplayManager replays;

    public MeldungBetterSmp(ReplayManager replays) {
        this.replays = replays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beiMeldung(SpielerGemeldetEvent event) {
        replays.spielerGemeldet(event.gemeldet(), event.gemeldetName());
    }
}
