package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * Tod und Respawn.
 *
 * <p>Hier steckt die Kernregel des Projekts: ein Herz kostet nur der Tod durch
 * einen anderen Spieler. Sturz, Lava, Mobs, Hunger und Ertrinken sind
 * schmerzhaft, aber folgenlos.</p>
 */
public final class DeathListener implements Listener {

    private final HeldenPlugin plugin;

    public DeathListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        HeldenProfile profile = plugin.profiles().getOrCreate(victim);
        Player killer = resolveKiller(victim);

        event.setDeathMessage(null);

        if (killer != null && !killer.equals(victim)) {
            profile.addPvpDeath();
            plugin.profiles().getOrCreate(killer).addKill();

            plugin.messages().broadcastRaw("combat.death-pvp",
                    "%victim%", victim.getName(),
                    "%killer%", killer.getName());
            Compat.sound(killer, "entity.player.levelup", 0.8f, 1.6f);

            plugin.hearts().loseHeart(victim, killer);
        } else if (plugin.settings().pvpOnly()) {
            // Der Tod war "natuerlich" - im Projekt kostet das kein Herz.
            profile.addNaturalDeath();
            plugin.messages().broadcastRaw("combat.death-natural", "%victim%", victim.getName());
            plugin.messages().send(victim, "hearts.safe-death");
        } else {
            profile.addPvpDeath();
            plugin.messages().broadcastRaw("combat.death-natural", "%victim%", victim.getName());
            plugin.hearts().loseHeart(victim, null);
        }

        plugin.combat().clearLastAttacker(victim);
        plugin.combat().clearTag(victim);
        plugin.hud().updateAll();
    }

    /** Killer laut Server, sonst der letzte Angreifer innerhalb der Gutschriftzeit. */
    private Player resolveKiller(Player victim) {
        Player killer = victim.getKiller();
        if (killer != null) {
            return killer;
        }
        UUID attacker = plugin.combat().lastAttacker(victim);
        return attacker == null ? null : plugin.getServer().getPlayer(attacker);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Location spawn = plugin.settings().spawnOrDefault();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }

        // Erst nach dem Respawn-Tick, sonst greift das Setzen der Herzen nicht.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.game().enforceState(player);
            if (!plugin.game().isEliminated(plugin.profiles().getOrCreate(player))) {
                plugin.combat().protect(player, plugin.settings().respawnProtectionSeconds());
                if (plugin.settings().respawnProtectionSeconds() > 0) {
                    plugin.messages().send(player, "combat.spawn-protection",
                            "%seconds%", plugin.settings().respawnProtectionSeconds());
                }
            }
            plugin.hud().update(player);
        }, 1L);
    }
}
