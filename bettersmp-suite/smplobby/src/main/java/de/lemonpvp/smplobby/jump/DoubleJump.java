package de.lemonpvp.smplobby.jump;

import de.lemonpvp.smplobby.SMPLobby;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/**
 * Doppelsprung.
 *
 * Der Trick dahinter ist alt und immer noch der beste: Man erlaubt dem
 * Spieler das Fliegen, faengt aber den Moment ab, in dem er es
 * einschaltet - also den zweiten Druck auf die Leertaste. Statt zu
 * fliegen bekommt er einen Schubs.
 *
 * Wieder erlaubt wird das Fliegen erst, wenn er den Boden beruehrt.
 * Sonst haette man einen unendlichen Sprung, und aus der Lobby waere ein
 * Flugplatz geworden.
 */
public final class DoubleJump implements Listener {

    private final SMPLobby plugin;

    public DoubleJump(SMPLobby plugin) {
        this.plugin = plugin;
    }

    private boolean aktiv() {
        return plugin.getConfig().getBoolean("doppelsprung.aktiv", true);
    }

    /** Beim Ankommen und nach jeder Landung: Fliegen wieder freigeben. */
    public void erlaube(Player spieler) {
        if (!aktiv() || spieler.getGameMode() == GameMode.CREATIVE
                || spieler.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        spieler.setAllowFlight(true);
        spieler.setFlying(false);
    }

    @EventHandler
    public void beimAbheben(PlayerToggleFlightEvent ereignis) {
        Player spieler = ereignis.getPlayer();
        if (!aktiv() || !ereignis.isFlying()) {
            return;
        }
        // Im Kreativmodus soll Fliegen Fliegen bleiben - dort baut der
        // Admin gerade an der Lobby.
        if (spieler.getGameMode() == GameMode.CREATIVE
                || spieler.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        ereignis.setCancelled(true);
        spieler.setAllowFlight(false);
        spieler.setFlying(false);

        double weite = plugin.getConfig().getDouble("doppelsprung.weite", 1.1);
        double hoehe = plugin.getConfig().getDouble("doppelsprung.hoehe", 0.9);
        Vector schub = spieler.getLocation().getDirection().multiply(weite).setY(hoehe);
        spieler.setVelocity(schub);
        spieler.playSound(Sound.sound(Key.key("minecraft:entity.bat.takeoff"),
                Sound.Source.PLAYER, 0.6f, 1.4f));
    }

    /**
     * Nach der Landung wieder freigeben.
     *
     * Das haengt am Bewegungsereignis, weil es kein "ist gelandet" gibt.
     * Geprueft wird nur, wenn der Spieler den Block gewechselt hat -
     * sonst liefe das bei jeder Kopfdrehung mit.
     */
    @EventHandler
    public void beiBewegung(PlayerMoveEvent ereignis) {
        if (!aktiv() || ereignis.getTo().getBlock().equals(ereignis.getFrom().getBlock())) {
            return;
        }
        Player spieler = ereignis.getPlayer();
        if (!spieler.isOnGround() || spieler.getAllowFlight()) {
            return;
        }
        erlaube(spieler);
    }
}
