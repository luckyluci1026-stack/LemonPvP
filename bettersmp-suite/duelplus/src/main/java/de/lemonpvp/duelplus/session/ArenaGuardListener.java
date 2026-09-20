package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Wacht ueber alles, was WAEHREND eines laufenden Duells bzw. innerhalb
 * einer Arena-Welt passiert:
 *
 *  - Ein toedlicher Treffer wird abgefangen statt einen echten Tod
 *    zuzulassen - die "Todeskamera" ist deshalb kein Vanilla-Tod,
 *    sondern eine eigene, komplett kontrollierte Sequenz (siehe
 *    DuellSessionManager.niederlageAusloesen). Waehrend des Countdowns
 *    (kampfLaeuft() noch false) macht das rundherum unverwundbar. Ein
 *    Totem der Unsterblichkeit in Haupt- oder Nebenhand rettet trotzdem
 *    ganz normal (vanilla) - wir greifen dann bewusst NICHT ein.
 *  - Waehrend des Countdowns stehen beide fest an ihrem Startpunkt -
 *    nur Umsehen bleibt erlaubt, Weglaufen (z.B. ueber den Rand) nicht.
 *  - Verbindung getrennt waehrend eines eigenen Duells = automatische
 *    Niederlage - verhindert, sich durch Abbrechen das eigene
 *    Inventar zu retten.
 *  - Befehle sind waehrend eines eigenen Duells komplett gesperrt
 *    (duelplus.command.bypass umgeht das, fuers Team) - einfacher und
 *    sicherer als einzelne Befehle wie /shop auf eine Sperrliste zu
 *    setzen.
 *  - Enderkisten sind in jeder Arena-Welt deaktiviert - es soll kein
 *    Loot ausserhalb der mitgebrachten Kampfausruestung geben.
 *  - Join-/Leave-Nachrichten sind auf dem Duels-Server generell stumm -
 *    hier landet ohnehin niemand, der nicht gerade duelliert.
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
        // Jeder verarbeitete Treffer zaehlt als "es wird gekaempft" - Basis
        // fuer die Camping-Erkennung der schrumpfenden Worldborder (siehe
        // DuellSessionManager.worldborderSchrumpfenStarten).
        session.treffer();
        double verbleibend = spieler.getHealth() - event.getFinalDamage();
        if (verbleibend > 0) {
            return;
        }
        if (haelTotem(spieler)) {
            // Nicht eingreifen: das ist dann ein Fall fuer Vanillas eigene
            // Totem-Rettung (EntityResurrectEvent), die sonst NIE ausgeloest
            // wuerde, weil wir das Damage-Event schon vorher abgefangen
            // haetten.
            return;
        }
        event.setCancelled(true);
        plugin.sessionManager().niederlageAusloesen(spieler.getUniqueId(), true);
    }

    private boolean haelTotem(Player spieler) {
        ItemStack haupt = spieler.getInventory().getItemInMainHand();
        ItemStack neben = spieler.getInventory().getItemInOffHand();
        return haupt.getType() == Material.TOTEM_OF_UNDYING || neben.getType() == Material.TOTEM_OF_UNDYING;
    }

    /**
     * Waehrend des Countdowns (kampfLaeuft() noch false) stehen beide fest
     * an ihrem Startpunkt und schauen sich an - nur Umsehen bleibt
     * erlaubt, Weglaufen nicht. Ueber PlayerMoveEvent#setTo statt eines
     * echten Teleports geloest, damit es nicht sichtbar ruckelt/"zurueck-
     * buggt".
     */
    @EventHandler(ignoreCancelled = true)
    public void beimBewegenWaehrendCountdown(PlayerMoveEvent event) {
        Location von = event.getFrom();
        Location zu = event.getTo();
        if (zu == null || (von.getX() == zu.getX() && von.getY() == zu.getY() && von.getZ() == zu.getZ())) {
            return;
        }
        Optional<DuellSession> sessionOpt = plugin.sessionManager().sessionVon(event.getPlayer().getUniqueId());
        if (sessionOpt.isEmpty() || sessionOpt.get().kampfLaeuft()) {
            return;
        }
        event.setTo(von.clone().setDirection(zu.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player spieler = event.getPlayer();
        if (plugin.sessionManager().sessionVon(spieler.getUniqueId()).isPresent()) {
            plugin.sessionManager().niederlageAusloesen(spieler.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBefehl(PlayerCommandPreprocessEvent event) {
        Player spieler = event.getPlayer();
        if (spieler.hasPermission("duelplus.command.bypass")) {
            return;
        }
        if (istBefehlsName(event.getMessage(), "draw")) {
            // /draw muss waehrend eines eigenen Duells gerade FUNKTIONIEREN
            // (das ist der ganze Sinn) - explizit von der Sperre ausgenommen.
            return;
        }
        if (plugin.sessionManager().sessionVon(spieler.getUniqueId()).isPresent()) {
            event.setCancelled(true);
            plugin.msgs().send(spieler, "command-blocked");
        }
    }

    private boolean istBefehlsName(String nachricht, String name) {
        String ohneSlash = nachricht.length() > 1 ? nachricht.substring(1) : "";
        int leerzeichen = ohneSlash.indexOf(' ');
        String befehl = leerzeichen < 0 ? ohneSlash : ohneSlash.substring(0, leerzeichen);
        return befehl.equalsIgnoreCase(name);
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
