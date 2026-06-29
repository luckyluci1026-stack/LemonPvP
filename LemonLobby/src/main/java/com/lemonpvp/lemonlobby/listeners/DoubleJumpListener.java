package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/**
 * Classic hub double-jump.
 *
 * <p>Lobby players press the jump key twice to launch forward + up with a cloud
 * burst and an airy whoosh. The double jump re-arms whenever the player is back
 * on the ground. Fall damage is cancelled so landings never hurt.</p>
 *
 * <p>Players who can use {@code /fly} ({@code lemoncore.use.fly}) keep normal
 * flight instead of double-jumping, so the two features don't fight.</p>
 *
 * <p>Tunable via config: {@code double-jump.enabled}, {@code double-jump.forward-power},
 * {@code double-jump.up-power}.</p>
 */
public class DoubleJumpListener implements Listener {

    private final LemonLobby plugin;

    public DoubleJumpListener(LemonLobby plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("double-jump.enabled", true);
    }

    private boolean canDoubleJump(Player p) {
        if (!enabled()) return false;
        GameMode gm = p.getGameMode();
        if (gm != GameMode.ADVENTURE && gm != GameMode.SURVIVAL) return false; // creative/spectator fly for real
        return !p.hasPermission("lemoncore.use.fly"); // /fly users keep real flight
    }

    /** Re-arm the double jump as soon as the player is back on the ground. */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getAllowFlight()) return;   // already armed (or flying) — cheap fast-path
        if (!p.isOnGround()) return;
        if (!canDoubleJump(p)) return;
        p.setAllowFlight(true);
    }

    /** The double-tap-jump fires a flight toggle — convert it into a launch. */
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (!event.isFlying()) return;    // stopping flight — ignore
        if (!canDoubleJump(p)) return;

        event.setCancelled(true);
        p.setAllowFlight(false);
        p.setFlying(false);

        double forward = plugin.getConfig().getDouble("double-jump.forward-power", 1.0);
        double up      = plugin.getConfig().getDouble("double-jump.up-power", 0.9);
        Vector boost = p.getLocation().getDirection().normalize().multiply(forward).setY(up);
        p.setVelocity(boost);

        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 18, 0.3, 0.05, 0.3, 0.05);
        p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation(), 8, 0.2, 0.1, 0.2, 0.08);
        p.playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.3f);
    }

    /** No fall damage in the lobby — double-jump landings (and drops) never hurt. */
    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!enabled()) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
