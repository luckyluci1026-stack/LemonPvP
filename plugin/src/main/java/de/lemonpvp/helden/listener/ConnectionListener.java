package de.lemonpvp.helden.listener;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.team.HeldenTeam;
import de.lemonpvp.helden.ui.HeroMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Join und Quit: Profil, HUD, Team, Willkommensnachricht und Combat-Log. */
public final class ConnectionListener implements Listener {

    private final HeldenPlugin plugin;

    public ConnectionListener(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HeldenProfile profile = plugin.profiles().getOrCreate(player);

        if (!profile.hasTeam() && plugin.teams().autoAssignEnabled()) {
            HeldenTeam team = plugin.teams().assignSmallest(profile);
            if (team != null) {
                plugin.messages().send(player, "team.joined", "%team%", team.display());
            }
        }

        plugin.hud().setup(player);
        plugin.lives().restoreOnJoin(player);
        plugin.combat().protect(player, plugin.settings().respawnProtectionSeconds());

        event.setJoinMessage(null);
        plugin.messages().broadcastRaw("join.join-broadcast", "%player%", player.getName());
        plugin.messages().sendList(player, "join.welcome",
                "%project%", plugin.settings().projectName(),
                "%player%", player.getName());

        if (plugin.settings().sendControlsHint() && plugin.bedrock().isBedrock(player)) {
            plugin.messages().send(player, "join.bedrock-hint");
        }

        if (!profile.hasHero() && plugin.settings().selectHeroOnFirstJoin()) {
            // Kurz warten, sonst schluckt der Client (vor allem Bedrock) das GUI.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    new HeroMenu(plugin).open(player);
                }
            }, 30L);
        } else {
            plugin.heroes().applyPassives(player);
            plugin.abilities().sendHint(player, plugin.heroes().of(profile));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        HeldenProfile profile = plugin.profiles().get(player);

        if (profile != null && shouldPunishCombatLog(player, profile)) {
            handleCombatLog(player);
        }

        if (profile != null) {
            profile.lastSeen(System.currentTimeMillis());
        }

        event.setQuitMessage(null);
        plugin.messages().broadcastRaw("join.quit-broadcast", "%player%", player.getName());

        plugin.hud().remove(player);
        plugin.combat().clear(player);
        plugin.abilities().clear(player);
    }

    private boolean shouldPunishCombatLog(Player player, HeldenProfile profile) {
        return plugin.settings().killOnCombatLog()
                && !profile.fallen()
                && plugin.combat().isTagged(player);
    }

    /**
     * Ausloggen im Kampf zaehlt als Tod: Leben weg, Inventar liegt am Ort des
     * Verschwindens, der letzte Angreifer bekommt die Gutschrift.
     */
    private void handleCombatLog(Player player) {
        plugin.messages().broadcastRaw("combat.combat-log", "%player%", player.getName());

        Location location = player.getLocation();
        if (location.getWorld() != null) {
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && !stack.getType().isAir()) {
                    location.getWorld().dropItemNaturally(location, stack);
                }
            }
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        UUID attackerId = plugin.combat().lastAttacker(player);
        Player attacker = attackerId == null ? null : plugin.getServer().getPlayer(attackerId);
        if (attacker != null) {
            plugin.combat().rewardKill(attacker, player);
        }
        plugin.combat().rewardAssists(player, attackerId);

        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        profile.addDeath();
        plugin.lives().handleDeath(player);
    }
}
