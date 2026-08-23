package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Locale;

/**
 * Setzt das Einfrieren durch: kein Laufen, kein Bauen, kein Abbauen,
 * kein Interagieren, kein Austeilen von Schaden.
 *
 * Umsehen bleibt erlaubt (nur die Position wird gesperrt, nicht der
 * Blickwinkel) und Reden auch - genau das braucht ein Teammitglied, um
 * mit der eingefrorenen Person zu klaeren, worum es geht.
 */
public final class FreezeListener implements Listener {

    private final BetterSMP plugin;

    public FreezeListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean eingefroren(Player spieler) {
        return plugin.freeze().istEingefroren(spieler.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void beiBewegung(PlayerMoveEvent event) {
        if (!eingefroren(event.getPlayer())) {
            return;
        }
        // Nur die Position sperren - sonst kann die eingefrorene Person
        // sich nicht einmal umsehen, um zu erkennen, wer da mit ihr redet.
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent event) {
        if (eingefroren(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimSetzen(BlockPlaceEvent event) {
        if (eingefroren(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void beimWegwerfen(PlayerDropItemEvent event) {
        if (eingefroren(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimSchadenAustragen(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player angreifer && eingefroren(angreifer)) {
            event.setCancelled(true);
        }
    }

    /**
     * Bewegungsbefehle waehrend des Einfrierens sperren - dieselbe Idee
     * wie bei den waehrend des Kampfes gesperrten Befehlen, nur eine
     * eigene, kuerzere Liste in freeze.blocked-commands.
     */
    @EventHandler(ignoreCancelled = true)
    public void beimBefehl(PlayerCommandPreprocessEvent event) {
        Player spieler = event.getPlayer();
        if (!eingefroren(spieler)) {
            return;
        }
        String wort = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int doppelpunkt = wort.indexOf(':');
        if (doppelpunkt >= 0) {
            wort = wort.substring(doppelpunkt + 1);
        }
        List<String> gesperrt = plugin.getConfig().getStringList("freeze.blocked-commands");
        if (gesperrt.stream().anyMatch(wort::equalsIgnoreCase)) {
            event.setCancelled(true);
            plugin.msgs().send(spieler, "freeze.command-blocked");
        }
    }

    /** Wer eingefroren die Verbindung trennt, ist es beim Wiederkommen immer noch. */
    @EventHandler
    public void beimBeitreten(PlayerJoinEvent event) {
        if (eingefroren(event.getPlayer())) {
            plugin.msgs().send(event.getPlayer(), "freeze.still-frozen");
        }
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        if (eingefroren(event.getPlayer())) {
            plugin.msgs().broadcast("freeze.left-while-frozen", "spieler", event.getPlayer().getName());
        }
    }
}
