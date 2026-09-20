package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * Wacht ueber alles, was WAEHREND eines laufenden Duells bzw. innerhalb
 * einer Arena-Welt passiert:
 *
 *  - Ein toedlicher Treffer wird abgefangen statt einen echten Tod
 *    zuzulassen - die "Todeskamera" ist deshalb kein Vanilla-Tod,
 *    sondern eine eigene, komplett kontrollierte Sequenz (siehe
 *    DuellSessionManager.niederlageAusloesen). Waehrend des Countdowns
 *    (kampfLaeuft() noch false) macht das rundherum unverwundbar.
 *  - Verbindung getrennt waehrend eines eigenen Duells = automatische
 *    Niederlage - verhindert, sich durch Abbrechen das eigene
 *    Inventar zu retten.
 *  - Befehle sind waehrend eines eigenen Duells komplett gesperrt
 *    (duelplus.command.bypass umgeht das, fuers Team) - einfacher und
 *    sicherer als einzelne Befehle wie /shop auf eine Sperrliste zu
 *    setzen.
 *  - Enderkisten sind in jeder Arena-Welt deaktiviert - es soll kein
 *    Loot ausserhalb der mitgebrachten Kampfausruestung geben.
 */
public final class ArenaGuardListener implements Listener {

    private final DuelPlus plugin;

    public ArenaGuardListener(DuelPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beiSchaden(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player spieler)) {
            return;
        }
        Optional<DuellSession> sessionOpt = plugin.sessionManager().sessionVon(spieler.getUniqueId());
        if (sessionOpt.isEmpty()) {
            return;
        }
        DuellSession session = sessionOpt.get();
        if (!session.kampfLaeuft()) {
            event.setCancelled(true);
            return;
        }
        double verbleibend = spieler.getHealth() - event.getFinalDamage();
        if (verbleibend > 0) {
            return;
        }
        event.setCancelled(true);
        plugin.sessionManager().niederlageAusloesen(spieler.getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        Player spieler = event.getPlayer();
        if (plugin.sessionManager().sessionVon(spieler.getUniqueId()).isPresent()) {
            plugin.sessionManager().niederlageAusloesen(spieler.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBefehl(PlayerCommandPreprocessEvent event) {
        Player spieler = event.getPlayer();
        if (spieler.hasPermission("duelplus.command.bypass")) {
            return;
        }
        if (plugin.sessionManager().sessionVon(spieler.getUniqueId()).isPresent()) {
            event.setCancelled(true);
            plugin.msgs().send(spieler, "command-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimInteragieren(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) {
            return;
        }
        if (!istArenaWelt(event.getPlayer().getWorld().getName())) {
            return;
        }
        event.setCancelled(true);
        plugin.msgs().send(event.getPlayer(), "enderchest-disabled");
    }

    private boolean istArenaWelt(String weltName) {
        String praefix = plugin.getConfig().getString("arenen.welt-praefix", "duell_arena_");
        return weltName.startsWith(praefix);
    }
}
