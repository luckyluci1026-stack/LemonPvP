package de.lemonpvp.smplobby.protect;

import de.lemonpvp.smplobby.SMPLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Die Lobby ist zum Anschauen da.
 *
 * Alles hier laesst sich in der config.yml einzeln abschalten, und wer
 * `smplobby.bauen` hat, kommt ueberall vorbei - sonst koennte der Admin
 * die Lobby nicht mehr umbauen, ohne das Plugin auszuschalten.
 *
 * Die Ereignisse werden auf HIGHEST abgefangen und nicht auf MONITOR:
 * MONITOR ist zum Zuschauen gedacht, was dort abgebrochen wird,
 * ueberrascht andere Plugins.
 */
public final class ProtectListener implements Listener {

    private final SMPLobby plugin;

    public ProtectListener(SMPLobby plugin) {
        this.plugin = plugin;
    }

    private boolean darfBauen(Player spieler) {
        return spieler.hasPermission("smplobby.bauen");
    }

    private boolean an(String schluessel) {
        // In der config.yml steht, was ERLAUBT ist. false heisst also
        // "verbieten" - deshalb hier die Umkehrung an genau einer Stelle,
        // statt in jeder Methode neu.
        return !plugin.getConfig().getBoolean("schutz." + schluessel, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent ereignis) {
        if (an("bloecke-abbauen") && !darfBauen(ereignis.getPlayer())) {
            ereignis.setCancelled(true);
            ereignis.getPlayer().sendMessage(plugin.msgs().format("nicht-bauen"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimSetzen(BlockPlaceEvent ereignis) {
        if (an("bloecke-setzen") && !darfBauen(ereignis.getPlayer())) {
            ereignis.setCancelled(true);
            ereignis.getPlayer().sendMessage(plugin.msgs().format("nicht-bauen"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beiSchaden(EntityDamageEvent ereignis) {
        if (!(ereignis.getEntity() instanceof Player spieler)) {
            return;
        }
        if (an("schaden") && !darfBauen(spieler)) {
            ereignis.setCancelled(true);
        }
    }

    /**
     * PvP getrennt vom uebrigen Schaden.
     *
     * Wer den allgemeinen Schaden anlaesst (weil in der Lobby ein
     * Parkour steht, bei dem man fallen darf), will trotzdem selten,
     * dass sich dreissig Leute gegenseitig verpruegeln.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beiPvp(EntityDamageByEntityEvent ereignis) {
        if (ereignis.getEntity() instanceof Player getroffen
                && ereignis.getDamager() instanceof Player schlaeger
                && an("pvp") && !darfBauen(schlaeger)) {
            ereignis.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beiHunger(FoodLevelChangeEvent ereignis) {
        if (ereignis.getEntity() instanceof Player spieler && an("hunger")) {
            ereignis.setCancelled(true);
            spieler.setFoodLevel(20);
            spieler.setSaturation(20f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimWegwerfen(PlayerDropItemEvent ereignis) {
        // Ohne das liegt die Schnellleiste nach fuenf Minuten auf dem
        // Boden verteilt und niemand findet mehr den Kompass.
        if (an("items-wegwerfen") && !darfBauen(ereignis.getPlayer())) {
            ereignis.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimAufheben(EntityPickupItemEvent ereignis) {
        if (ereignis.getEntity() instanceof Player spieler
                && an("items-aufheben") && !darfBauen(spieler)) {
            ereignis.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimSpawnen(CreatureSpawnEvent ereignis) {
        if (an("mobs-spawnen")) {
            ereignis.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimWetter(WeatherChangeEvent ereignis) {
        // Nur das Losregnen abfangen. Wuerde man auch das Aufklaren
        // abbrechen, bliebe ein einmal gestarteter Regen fuer immer.
        if (an("wetter") && ereignis.toWeatherState()) {
            ereignis.setCancelled(true);
        }
    }
}
