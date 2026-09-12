package de.lemonpvp.lifesteal.hook;

import de.lemonpvp.bettersmp.api.PlayerCombatLogEvent;
import de.lemonpvp.lifesteal.LifestealPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Brücke zu BetterSMP: Loggt ein Spieler im Kampf aus, wird das wie ein Tod
 * behandelt - Herzverlust für den Logger, Herzgewinn für den Gegner.
 *
 * Diese Klasse wird NUR geladen/registriert, wenn BetterSMP vorhanden ist
 * (siehe LifestealPlus#onEnable), daher ist der harte Import unproblematisch.
 */
public final class CombatLogBridge implements Listener {

    private final LifestealPlus plugin;

    public CombatLogBridge(LifestealPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCombatLog(PlayerCombatLogEvent event) {
        if (!plugin.getConfig().getBoolean("combatlog.enabled", true)) {
            return;
        }
        Player logger = event.getPlayer();
        UUID opponent = event.getOpponent();

        // Diesen kommenden Tod (BetterSMP setzt Health=0) nicht doppelt zählen
        plugin.addCombatLogGuard(logger.getUniqueId());

        String opponentName = opponent == null ? ""
                : String.valueOf(Bukkit.getOfflinePlayer(opponent).getName());
        plugin.killHandler().handle(logger, opponent, opponentName, opponent != null);
        plugin.msgs().broadcast("combatlog-death", "player", logger.getName());
    }
}
