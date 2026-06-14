package de.lemonpvp.flfac.violation;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.Check;
import de.lemonpvp.flfac.check.CheckType;
import de.lemonpvp.flfac.config.CheckSettings;
import de.lemonpvp.flfac.config.ConfigManager;
import de.lemonpvp.flfac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Central entry point for raising violations. Increments the violation level,
 * decides whether staff should be alerted and triggers punishments.
 */
public final class ViolationManager {

    private final FLFAC plugin;

    public ViolationManager(FLFAC plugin) {
        this.plugin = plugin;
    }

    public void flag(Check check, PlayerData data, String details) {
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player.hasPermission("flfac.bypass")) {
            return;
        }

        CheckType type = check.getType();
        ConfigManager config = plugin.getConfigManager();
        CheckSettings settings = config.getCheck(type);

        double vl = data.addViolations(type, settings.getVlAdd(), settings.getMaxVl());
        long now = System.currentTimeMillis();

        // Verbose: every single flag goes to verbose subscribers.
        plugin.getAlertManager().verbose(check, data, vl, details);

        // Alert: only once the alert threshold is reached, throttled per check.
        if (vl >= settings.getAlertVl()
                && !data.isOnAlertCooldown(type, config.getAlertCooldownMs(), now)) {
            plugin.getAlertManager().alert(check, data, vl, details);
        }

        // Punishments.
        if (config.isPunishmentsEnabled()) {
            plugin.getPunishmentManager().handle(type, data, vl);
        }
    }
}
