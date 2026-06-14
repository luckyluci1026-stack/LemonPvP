package de.lemonpvp.flfac.listener;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckManager;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/** Drives the movement and packet checks from {@link PlayerMoveEvent}. */
public final class MovementListener implements Listener {

    private final FLFAC plugin;

    public MovementListener(FLFAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);
        CheckManager checks = plugin.getCheckManager();

        // Packet sanity - always, very cheap.
        checks.badPackets().handleMove(data, to);

        // Timer - cheap, but skip under lag to avoid false positives.
        if (plugin.shouldRunHeavyChecks()) {
            checks.timer().handleMove(data);
        }

        // Keep rotation history up to date for KillAura.
        data.updateRotation(to.getYaw(), to.getPitch());

        boolean positionChanged = from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ();

        if (positionChanged) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double dz = to.getZ() - from.getZ();
            boolean onGround = player.isOnGround();

            // Velocity is light and must only run on real movement.
            checks.velocity().handleMove(data, dx, dy, dz);

            if (plugin.shouldRunHeavyChecks()) {
                checks.speed().handleMove(data, player, dx, dz, onGround);
                checks.fly().handleMove(data, player, dy, onGround);
                checks.noFall().handleMove(data, player, dy, onGround);
                checks.jesus().handleMove(data, player, dx, dy, dz, onGround);
            }

            data.setLastDeltaY(dy);
            data.setLastOnGround(onGround);
            data.pushLocation(to.clone());
        }
    }
}
