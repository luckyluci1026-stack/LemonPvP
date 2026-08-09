package de.lemonpvp.bettersmp.combat;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.api.PlayerCombatLogEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * AntiCombatLog: taggt PvP-Teilnehmer, sperrt Befehle im Kampf und
 * bestraft Combat-Logging (Tod + Broadcast + API-Event für Lifesteal+).
 */
public final class CombatListener implements Listener {

    private final BetterSMP plugin;
    private final CombatManager combat;

    public CombatListener(BetterSMP plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("combat.enabled", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled() || event.getFinalDamage() <= 0) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (attacker.hasPermission("bettersmp.combat.bypass")
                || victim.hasPermission("bettersmp.combat.bypass")) {
            return;
        }
        combat.tag(victim, attacker);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        if (event.getDamager() instanceof AreaEffectCloud cloud
                && cloud.getSource() instanceof Player source) {
            return source;
        }
        if (event.getDamager() instanceof TNTPrimed tnt
                && tnt.getSource() instanceof Player igniter) {
            return igniter;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        UUID dead = event.getPlayer().getUniqueId();
        UUID opponent = combat.opponent(dead);
        combat.untag(dead);
        if (opponent != null && dead.equals(combat.opponent(opponent))) {
            combat.release(opponent);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!enabled() || !combat.isTagged(player.getUniqueId())) {
            return;
        }
        UUID opponent = combat.opponent(player.getUniqueId());
        combat.untag(player.getUniqueId());

        new PlayerCombatLogEvent(player, opponent).callEvent();

        String punishment = plugin.getConfig().getString("combat.punishment", "KILL")
                .toUpperCase(Locale.ROOT);
        if (punishment.equals("KILL") && !player.isDead()) {
            player.setHealth(0.0);
        }
        plugin.msgs().broadcast("combat.logged-broadcast", "player", player.getName());

        if (opponent != null
                && plugin.getConfig().getBoolean("combat.untag-opponent-on-log", true)
                && player.getUniqueId().equals(combat.opponent(opponent))) {
            combat.untag(opponent);
            Player other = Bukkit.getPlayer(opponent);
            if (other != null) {
                plugin.msgs().send(other, "combat.opponent-logged", "player", player.getName());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled() || !combat.isTagged(event.getPlayer().getUniqueId())) {
            return;
        }
        String root = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = root.indexOf(':');
        if (colon >= 0) {
            root = root.substring(colon + 1);
        }
        List<String> blocked = plugin.getConfig().getStringList("combat.blocked-commands");
        if (blocked.stream().anyMatch(root::equalsIgnoreCase)) {
            event.setCancelled(true);
            long seconds = (combat.remainingMillis(event.getPlayer().getUniqueId()) + 999) / 1000;
            plugin.msgs().send(event.getPlayer(), "combat.command-blocked",
                    "seconds", String.valueOf(seconds));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("combat.block-elytra", true)) {
            return;
        }
        if (event.isGliding() && event.getEntity() instanceof Player player
                && combat.isTagged(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
