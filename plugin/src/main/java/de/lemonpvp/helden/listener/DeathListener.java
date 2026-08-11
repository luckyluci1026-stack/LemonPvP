package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/** Tod und Respawn: Statistik, Belohnungen, Leben und Kit. */
public final class DeathListener implements Listener {

    private final HeldenPlugin plugin;

    public DeathListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        HeldenProfile victimProfile = plugin.profiles().getOrCreate(victim);
        Player killer = resolveKiller(victim);

        int endedStreak = victimProfile.killStreak();
        victimProfile.addDeath();
        plugin.economy().withdrawSilently(victimProfile, plugin.settings().lossPerDeath());

        event.setDeathMessage(null);
        announceDeath(victim, killer);

        if (killer != null && !killer.equals(victim)) {
            plugin.combat().rewardKill(killer, victim);
            if (endedStreak >= plugin.settings().announceKillstreakFrom()) {
                plugin.messages().broadcastRaw("combat.streak-ended",
                        "%player%", killer.getName(),
                        "%victim%", victim.getName(),
                        "%streak%", endedStreak);
            }
        }
        plugin.combat().rewardAssists(victim, killer == null ? null : killer.getUniqueId());

        plugin.lives().handleDeath(victim);
        plugin.combat().clearContributors(victim);
        plugin.combat().clearTag(victim);
        plugin.abilities().clear(victim);
        plugin.hud().updateAll();
    }

    private void announceDeath(Player victim, Player killer) {
        if (killer != null && !killer.equals(victim)) {
            Hero killerHero = plugin.heroes().of(killer);
            plugin.messages().broadcastRaw("combat.death-pvp",
                    "%victim%", victim.getName(),
                    "%killer%", killer.getName(),
                    "%hero%", killerHero == null ? "-" : killerHero.display());
        } else {
            plugin.messages().broadcastRaw("combat.death-generic", "%victim%", victim.getName());
        }
    }

    /** Killer laut Server, sonst der letzte Angreifer aus dem Combat-Log. */
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

        Location spawn = plugin.spawnFor(player);
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }

        // Nach dem Respawn-Tick, sonst laufen Kit und Effekte ins Leere.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            HeldenProfile profile = plugin.profiles().getOrCreate(player);
            if (profile.fallen()) {
                plugin.lives().applyFallenState(player);
                return;
            }
            Hero hero = plugin.heroes().of(profile);
            if (hero != null) {
                if (plugin.settings().giveKitOnRespawn()) {
                    plugin.heroes().giveKit(player, hero);
                }
                plugin.heroes().applyPassives(player, hero);
            }
            plugin.combat().protect(player, plugin.settings().respawnProtectionSeconds());
            plugin.messages().send(player, "combat.spawn-protection",
                    "%seconds%", plugin.settings().respawnProtectionSeconds());
            plugin.hud().update(player);
        }, 1L);
    }
}
