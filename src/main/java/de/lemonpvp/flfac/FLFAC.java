package de.lemonpvp.flfac;

import de.lemonpvp.flfac.alert.AlertManager;
import de.lemonpvp.flfac.check.CheckManager;
import de.lemonpvp.flfac.command.FLFACCommand;
import de.lemonpvp.flfac.config.ConfigManager;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.data.PlayerDataManager;
import de.lemonpvp.flfac.listener.CombatListener;
import de.lemonpvp.flfac.listener.ConnectionListener;
import de.lemonpvp.flfac.listener.MovementListener;
import de.lemonpvp.flfac.listener.WorldListener;
import de.lemonpvp.flfac.punish.PunishmentManager;
import de.lemonpvp.flfac.util.ColorUtil;
import de.lemonpvp.flfac.violation.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * FLFAC - Fast Lag Free Anti Cheat.
 *
 * <p>A GrimAC-inspired, event-driven anticheat for Paper with gradient styling
 * and a configurable set of combat, movement and world checks.</p>
 */
public final class FLFAC extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private AlertManager alertManager;
    private PunishmentManager punishmentManager;

    private BukkitTask tickTask;
    private long tick;
    private volatile double currentTps = 20.0D;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.playerDataManager = new PlayerDataManager();
        this.alertManager = new AlertManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.violationManager = new ViolationManager(this);
        this.checkManager = new CheckManager(this);

        registerListeners();
        registerCommands();
        startScheduler();

        // Re-track already online players (e.g. after /reload).
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = playerDataManager.getOrCreate(player);
            data.setAlertsEnabled(player.hasPermission("flfac.alerts") && configManager.isAlertsEnabledByDefault());
        }

        printBanner();
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (playerDataManager != null) {
            playerDataManager.clear();
        }
        if (punishmentManager != null) {
            punishmentManager.clearAll();
        }
        getLogger().info("FLFAC disabled. Stay lag free.");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MovementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("flfac");
        if (command != null) {
            FLFACCommand executor = new FLFACCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void startScheduler() {
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            tick++;
            if (tick % 20L == 0L) {
                double[] tps = Bukkit.getTPS();
                currentTps = tps.length > 0 ? Math.min(20.0D, tps[0]) : 20.0D;

                double decay = configManager.getDecayPerSecond();
                if (decay > 0.0D) {
                    for (PlayerData data : playerDataManager.all()) {
                        data.decay(decay);
                    }
                }
            }
        }, 1L, 1L);
    }

    /** Heavy checks are skipped under low TPS to keep the server lag free. */
    public boolean shouldRunHeavyChecks() {
        double minTps = configManager.getMinTps();
        return minTps <= 0.0D || currentTps >= minTps;
    }

    public void reload() {
        configManager.load();
    }

    private void printBanner() {
        if (!configManager.isStartupBanner()) {
            getLogger().info("FLFAC enabled - " + checkManager.enabledCount() + "/" + checkManager.totalCount() + " checks active.");
            return;
        }
        String[] lines = {
                "  ___ _    ___ _   ___ ",
                " | __| |  | __/_\\ / __|",
                " | _|| |__| _/ _ \\ (__ ",
                " |_| |____|_/_/ \\_\\___|",
                " Fast Lag Free Anti Cheat  v" + getDescription().getVersion()
        };
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.gradient(line, configManager.getPrefixGradient()));
        }
        Bukkit.getConsoleSender().sendMessage(ColorUtil.gradient(
                " " + checkManager.enabledCount() + "/" + checkManager.totalCount() + " checks active  |  by LemonPvP",
                configManager.getAccentGradient()));
    }

    public long getTick() {
        return tick;
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
}
